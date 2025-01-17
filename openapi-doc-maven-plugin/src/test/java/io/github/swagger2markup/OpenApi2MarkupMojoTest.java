package io.github.swagger2markup;

import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApi2MarkupMojoTest {

    private static final String RESOURCES_DIR = "src/test/resources";
    private static final String SWAGGER_DIR = "/docs/swagger";
    private static final String INPUT_DIR = RESOURCES_DIR + SWAGGER_DIR;
    private static final String SWAGGER_OUTPUT_FILE = "swagger";
    private static final String SWAGGER_INPUT_FILE = "swagger.json";
    private static final String OUTPUT_DIR = "target/generated-docs";
    private File outputDir;

    @BeforeEach
    public void clearGeneratedData() throws Exception {
        outputDir = new File(OUTPUT_DIR);
        FileUtils.deleteQuietly(outputDir);
    }

    @Test
    public void shouldSkipExecution() throws Exception {
        //given
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.outputFile = new File(OUTPUT_DIR, SWAGGER_OUTPUT_FILE).getAbsoluteFile();
        mojo.skip = true;

        //when
        mojo.execute();

        //then
        assertThat(mojo.outputFile).doesNotExist();
    }

    @Test
    public void shouldConvertIntoFile() throws Exception {
        //given
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputFile = new File(OUTPUT_DIR, SWAGGER_OUTPUT_FILE).getAbsoluteFile();
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(outputDir);
        assertThat(outputFiles).containsOnly("swagger.adoc");
    }

    @Test
    public void shouldConvertIntoDirectory() throws Exception {
        //given
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void shouldConvertIntoDirectoryIfInputIsDirectory() throws Exception {
        //given that the input folder contains a nested structure with Swagger files
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(RESOURCES_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(new File(mojo.outputDir, SWAGGER_DIR));
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
        outputFiles = listFileNames(new File(mojo.outputDir, SWAGGER_DIR + "2"), false);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void shouldConvertIntoDirectoryIfInputIsDirectoryWithMixedSeparators() throws Exception {
        //given that the input folder contains a nested structure with Swagger files but path to it contains mixed file
        //separators on Windows (/ and \)
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        String openApiInputPath = new File(RESOURCES_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.openApiInput = replaceLast(openApiInputPath, "\\", "/");
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(new File(mojo.outputDir, SWAGGER_DIR));
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
        outputFiles = listFileNames(new File(mojo.outputDir, SWAGGER_DIR + "2"), false);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void shouldConvertIntoSubDirectoryIfMultipleSwaggerFilesInSameInput() throws Exception {
        //given that the input folder contains two Swagger files
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        List<String> directoryNames = Arrays.asList(mojo.outputDir.listFiles()).stream().map(File::getName)
                                            .collect(Collectors.toList());
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
        assertThat(outputFiles.spliterator().getExactSizeIfKnown()).isEqualTo(8); // same set of files twice
        assertThat(directoryNames).containsOnly("swagger", "swagger2");
    }

    @Test
    public void shouldConvertIntoSubDirectoryOneFileIfMultipleSwaggerFilesInSameInput() throws Exception {
        //given that the input folder contains two Swagger files
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.outputFile = new File(SWAGGER_OUTPUT_FILE);
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        List<String> directoryNames = Arrays.asList(mojo.outputDir.listFiles()).stream().map(File::getName)
                                            .collect(Collectors.toList());
        assertThat(outputFiles).containsOnly("swagger.adoc");
        assertThat(outputFiles.spliterator().getExactSizeIfKnown()).isEqualTo(2); // same set of files twice
        assertThat(directoryNames).containsOnly("swagger", "swagger2");
    }

    @Test
    public void shouldConvertIntoMarkdown() throws Exception {
        //given
        Map<String, String> config = new HashMap<>();
        config.put(OpenApi2MarkupProperties.MARKUP_LANGUAGE, MarkupLanguage.MARKDOWN.toString());

        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.config = config;
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        assertThat(outputFiles).containsOnly("definitions.md", "overview.md", "paths.md", "security.md");
    }

    @Test
    public void shouldConvertFromUrl() throws Exception {
        //given
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = "http://petstore.swagger.io/v2/swagger.json";
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.swagger = true;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void testMissingInputDirectory() throws Exception {
        //given
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR, "non-existent").getAbsoluteFile().getAbsolutePath();

        //when
        Assertions.assertThrows(MojoFailureException.class, () -> {
        mojo.execute();
        });
    }

    @Test
    public void testUnreadableOutputDirectory() throws Exception {
        //given
        OpenApi2MarkupMojo mojo = new OpenApi2MarkupMojo();
        mojo.openApiInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = Mockito.mock(File.class, (Answer) invocationOnMock -> {
            if (!invocationOnMock.getMethod().getName().contains("toString")) {
                throw new IOException("test exception");
            }
            return null;
        });

        //when
        Assertions.assertThrows(MojoFailureException.class, () -> {
          mojo.execute();
        });
    }


    private static Iterable<String> recursivelyListFileNames(File dir) throws Exception {
        return listFileNames(dir, true);
    }

    private static Iterable<String> listFileNames(File dir, boolean recursive) {
        return FileUtils.listFiles(dir, null, recursive).stream().map(File::getName).collect(Collectors.toList());
    }

    private static void verifyFileContains(File file, String value) throws IOException {
        assertThat(IOUtils.toString(file.toURI(), StandardCharsets.UTF_8)).contains(value);
    }

    private static String replaceLast(String input, String search, String replace) {
        int lastIndex = input.lastIndexOf(search);
        if (lastIndex > -1) {
            return input.substring(0, lastIndex)
                    + replace
                    + input.substring(lastIndex + search.length(), input.length());
        } else {
            return input;
        }
    }
}
