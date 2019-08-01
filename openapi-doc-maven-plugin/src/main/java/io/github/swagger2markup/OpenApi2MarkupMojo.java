/*
 * Copyright 2016 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.swagger2markup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import io.github.swagger2markup.OpenApi2MarkupConverter.Builder;
import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.utils.URIUtils;

/**
 * Basic mojo to invoke the {@link OpenApi2MarkupConverter}
 * during the maven build cycle
 */
@Mojo(name = "openapi2markup", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class OpenApi2MarkupMojo extends AbstractMojo {

    /**
     * The enclosing project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    @Parameter(property = "openApiInput", required = true)
    protected String openApiInput;

    @Parameter(property = "outputDir")
    protected File outputDir;

    @Parameter(property = "outputFile")
    protected File outputFile;

    @Parameter
    protected Map<String, String> config = new HashMap<>();

    @Parameter(property = "skip")
    protected boolean skip;

    @Parameter(property = "swagger")
    protected boolean swagger;

    @Parameter(property = "overrideLabelsFile")
    private String overrideLabelsFile;

    @Parameter(property = "overridePropertiesFile")
    private String overridePropertiesFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("openapi2markup is skipped.");
            return;
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("openapi2markup goal started");
            getLog().debug("openApiInput: " + openApiInput);
            getLog().debug("outputDir: " + outputDir);
            getLog().debug("outputFile: " + outputFile);
            for (Map.Entry<String, String> entry : this.config.entrySet()) {
                getLog().debug(entry.getKey() + ": " + entry.getValue());
            }
        }

        // includes config overlay
        Map<String, String> overrideProps = processOverrides();

        try {
            OpenApi2MarkupConfigBuilder configBuilder = new OpenApi2MarkupConfigBuilder(overrideProps);
            if (overrideLabelsFile != null) {
              configBuilder.withLabelsOverride(getOverridenLabels());
            }
            OpenApi2MarkupConfig openApi2MarkupConfig = configBuilder.build();
            if (isLocalFolder(openApiInput)) {
              getOpenApiFiles(new File(openApiInput), true).forEach(f -> {
                Builder converter = null;
                if (swagger) {
                  converter = OpenApi2MarkupConverter.fromSwagger(f.toURI());
                } else {
                  converter = OpenApi2MarkupConverter.from(f.toURI());
                }
                openApiToMarkup(converter.withConfig(openApi2MarkupConfig).build(), true);
              });
            } else {
              Builder converter = null;
              if (swagger) {
                converter = OpenApi2MarkupConverter.fromSwagger(URIUtils.create(openApiInput));
              } else {
                converter = OpenApi2MarkupConverter.from(URIUtils.create(openApiInput));
              }
              openApiToMarkup(converter.withConfig(openApi2MarkupConfig).build(), false);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Failed to execute goal 'openapi2markup'", e);
        }
        getLog().debug("openapi2markup goal finished");
    }

    private Map<String, String> getOverridenLabels() throws MojoExecutionException {
      Properties props = propertiesToMap(overrideLabelsFile);
      Map<String, String> map = new HashMap<>();
      map.putAll(props.entrySet().stream().collect(
          Collectors.toMap(
              e -> e.getKey().toString(),
              e -> e.getValue().toString())));
      return map;
    }

    private static boolean isLocalFolder(String openApiInput) {
        return !openApiInput.toLowerCase().startsWith("http") && new File(openApiInput).isDirectory();
    }

    private void openApiToMarkup(OpenApi2MarkupConverter converter, boolean inputIsLocalFolder) {
        if (outputFile != null) {
            Path useFile = outputFile.toPath();
            /*
             * If user has specified input folder with multiple files to convert,
             * and has specified a single output file, then route all conversions
             * into one file under each 'new' sub-directory, which corresponds to
             * each input file.
             * Otherwise, specifying the output file with an input DIRECTORY means
             * last file converted wins.
             */
            if (inputIsLocalFolder) {
                if ( outputDir != null ) {
                   File effectiveOutputDir = outputDir;
                   effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(converter);
                   converter.getContext().setOutputPath(effectiveOutputDir.toPath());
                   useFile =  Paths.get(effectiveOutputDir.getPath(), useFile.getFileName().toString());
                }
            }
            if ( getLog().isInfoEnabled() ) {
               getLog().info("Converting input to one file: " + useFile);
            }
            converter.toFile(useFile);
        } else if (outputDir != null) {
            File effectiveOutputDir = outputDir;
            if (inputIsLocalFolder) {
                effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(converter);
            }
            if (getLog().isInfoEnabled()) {
               getLog().info("Converting input to multiple files in folder: '" + effectiveOutputDir + "'");
            }
            converter.toFolder(effectiveOutputDir.toPath());
        } else {
            throw new IllegalArgumentException("Either outputFile or outputDir parameter must be used");
        }
    }

    private File getEffectiveOutputDirWhenInputIsAFolder(OpenApi2MarkupConverter converter) {
        String outputDirAddendum = getInputDirStructurePath(converter);
        if (multipleOpenApiFilesInOpenApiLocationFolder(converter)) {
            /*
             * If the folder the current OpenApi file resides in contains at least one other OpenApi file then the
             * output dir must have an extra subdir per file to avoid markdown files getting overwritten.
             */
            outputDirAddendum += File.separator + extractOpenApiFileNameWithoutExtension(converter);
        }
        return new File(outputDir, outputDirAddendum);
    }

    private String getInputDirStructurePath(OpenApi2MarkupConverter converter) {
        /*
         * When the OpenApi input is a local folder (e.g. /Users/foo/) you'll want to group the generated output in the
         * configured output directory. The most obvious approach is to replicate the folder structure from the input
         * folder to the output folder. Example:
         * - openApiInput is set to /Users/foo
         * - there's a single OpenApi file at /Users/foo/bar-service/v1/bar.yaml
         * - outputDir is set to /tmp/asciidoc
         * -> markdown files from bar.yaml are generated to /tmp/asciidoc/bar-service/v1
         */
        String openApiFilePath = new File(converter.getContext().getOpenApiLocation()).getAbsolutePath(); // /Users/foo/bar-service/v1/bar.yaml
        String openApiFileFolder = StringUtils.substringBeforeLast(openApiFilePath, File.separator); // /Users/foo/bar-service/v1
        return StringUtils.remove(openApiFileFolder, getOpenApiInputAbsolutePath()); // /bar-service/v1
    }

    private static boolean multipleOpenApiFilesInOpenApiLocationFolder(OpenApi2MarkupConverter converter) {
        Collection<File> openApiFiles = getOpenApiFiles(new File(converter.getContext().getOpenApiLocation())
          .getParentFile(), false);
        return openApiFiles != null && openApiFiles.size() > 1;
    }

    private static String extractOpenApiFileNameWithoutExtension(OpenApi2MarkupConverter converter) {
        return FilenameUtils.removeExtension(new File(converter.getContext().getOpenApiLocation()).getName());
    }

    private static Collection<File> getOpenApiFiles(File directory, boolean recursive) {
        return FileUtils.listFiles(directory, new String[]{"yaml", "yml", "json"}, recursive);
    }

    /*
     * The 'openApiInput' provided by the user can be anything; it's just a string. Hence, it could by Unix-style,
     * Windows-style or even a mix thereof. This methods turns the input into a File and returns its absolute path. It
     * will be platform dependent as far as file separators go but at least the separators will be consistent.
     */
    private String getOpenApiInputAbsolutePath(){
        return new File(openApiInput).getAbsolutePath();
    }

    private Map<String, String> processOverrides() throws MojoExecutionException {
      Properties props = propertiesToMap(overridePropertiesFile);
      Map<String, String> map = new HashMap<>();
      map.putAll(props.entrySet().stream().collect(
          Collectors.toMap(
              e -> e.getKey().toString(),
              e -> e.getValue().toString())));

      if (config != null) {
        map.putAll(config.entrySet().stream().collect(
            Collectors.toMap(
                e -> e.getKey().toString(),
                e -> e.getValue().toString())));
      }
      return map;
    }

    private Properties propertiesToMap(String fileDesc) throws MojoExecutionException {
      Properties props = new Properties();
      if (fileDesc != null) {
        Path file = Paths.get(fileDesc);
        if (!file.isAbsolute()) {
          file = project.getBasedir().toPath().resolve(file);
        }
        getLog().info("Loading properties from : " + file);
        try {
          props.load(Files.newBufferedReader(file, StandardCharsets.UTF_8));
        } catch (IOException e1) {
          throw new MojoExecutionException("Error loading override file: ", e1);
        }
      }
      return props;
    }
}
