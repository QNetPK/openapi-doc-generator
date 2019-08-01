/*
 * Copyright 2017 Robert Winkler
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

import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.builder.OpenApi2MarkupExtensionRegistryBuilder;
import io.github.swagger2markup.internal.document.DefinitionsDocument;
import io.github.swagger2markup.internal.document.OverviewDocument;
import io.github.swagger2markup.internal.document.PathsDocument;
import io.github.swagger2markup.internal.document.SecurityDocument;
import io.github.swagger2markup.internal.utils.ModelUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.markup.builder.MarkupDocBuilders;
import io.github.swagger2markup.utils.URIUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Robert Winkler
 */
public class OpenApi2MarkupConverter {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApi2MarkupConverter.class);

    private final Context context;
    private final OverviewDocument overviewDocument;
    private final PathsDocument pathsDocument;
    private final DefinitionsDocument definitionsDocument;
    private final SecurityDocument securityDocument;

    public OpenApi2MarkupConverter(Context context) {
        this.context = context;
        this.overviewDocument = new OverviewDocument(context);
        this.pathsDocument = new PathsDocument(context);
        this.definitionsDocument = new DefinitionsDocument(context);
        this.securityDocument = new SecurityDocument(context);
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a URI.
     *
     * @param openApiUri the URI
     * @return a OpenApi2MarkupConverter
     */
    public static Builder from(URI openApiUri) {
        LOG.info("Parsing OpenAPI V3+");
        Validate.notNull(openApiUri, "openApiUri must not be null");
        String scheme = openApiUri.getScheme();
        if (scheme != null && openApiUri.getScheme().startsWith("http")) {
            try {
                return from(openApiUri.toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to convert URI to URL", e);
            }
        } else if (scheme != null && openApiUri.getScheme().startsWith("file")) {
            return from(Paths.get(openApiUri));
        } else {
            return from(URIUtils.convertUriWithoutSchemeToFileScheme(openApiUri));
        }
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a Swagger V2 URI.
     *
     * @param swaggerUri the URI
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder fromSwagger(URI swaggerUri) {
        LOG.info("Parsing Swagger V2");
        Validate.notNull(swaggerUri, "swaggerUri must not be null");
        String scheme = swaggerUri.getScheme();
        if (scheme != null && swaggerUri.getScheme().startsWith("http")) {
            try {
                return fromSwagger(swaggerUri.toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to convert URI to URL", e);
            }
        } else if (scheme != null && swaggerUri.getScheme().startsWith("file")) {
            return fromSwagger(Paths.get(swaggerUri));
        } else {
            return fromSwagger(URIUtils.convertUriWithoutSchemeToFileScheme(swaggerUri));
        }
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder using a remote URL.
     *
     * @param openApiURL the remote URL
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder from(URL openApiURL) {
        LOG.info("Parsing OpenAPI V3+");
        Validate.notNull(openApiURL, "openApiURL must not be null");
        return new Builder(openApiURL);
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder using a remote Swagger V2 URL.
     *
     * @param swaggerURL the remote URL
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder fromSwagger(URL swaggerURL) {
        LOG.info("Parsing Swagger V2");
        Validate.notNull(swaggerURL, "swaggerURL must not be null");
        return new BuilderV2(swaggerURL);
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder using a local Path.
     *
     * @param openApiPath the local Path
     * @return a OpenApi2MarkupConverter
     */
    public static Builder from(Path openApiPath) {
        LOG.info("Parsing OpenAPI V3+");
        Validate.notNull(openApiPath, "openApiPath must not be null");
        verifyPath(openApiPath, "openApiPath");
        return new Builder(openApiPath);
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder using a local Swagger V2 Path.
     *
     * @param swaggerPath the local Swagger V2 Path
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder fromSwagger(Path swaggerPath) {
        LOG.info("Parsing Swagger V2");
        verifyPath(swaggerPath, "swaggerPath");
        return new BuilderV2(swaggerPath);
    }

    private static void verifyPath(Path path, String caller) {
      Validate.notNull(path, caller + " must not be null");
      if (Files.notExists(path)) {
          throw new IllegalArgumentException(String.format("%s does not exist: %s", caller, path));
      }
      try {
          if (Files.isHidden(path)) {
              throw new IllegalArgumentException(caller + " must not be a hidden file");
          }
      } catch (IOException e) {
          throw new RuntimeException("Failed to check if " + caller + " is a hidden file", e);
      }
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a given OpenAPI model.
     *
     * @param openApi the OpenAPI source.
     * @return a OpenApi2MarkupConverter
     */
    public static Builder from(OpenAPI openApi) {
        LOG.info("Using parsed OpenAPI V3+");
        Validate.notNull(openApi, "openApi must not be null");
        return new Builder(openApi);
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a given OpenAPI YAML or JSON String.
     *
     * @param openApiString the OpenAPI YAML or JSON String.
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder from(String openApiString) {
        Validate.notEmpty(openApiString, "openApiString must not be null");
        return from(new StringReader(openApiString));
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a given Swagger YAML or JSON String.
     *
     * @param openApiString the Swagger YAML or JSON String.
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder fromSwagger(String swaggerString) {
        Validate.notEmpty(swaggerString, "openApiString must not be null");
        return fromSwagger(new StringReader(swaggerString));
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a given OpenAPI YAML or JSON reader.
     *
     * @param openApiReader the OpenAPI YAML or JSON reader.
     * @return a OpenApi2MarkupConverter
     */
    public static Builder from(Reader openApiReader) {
        Validate.notNull(openApiReader, "openApiReader must not be null");
        OpenAPI openApi;
        try {
            openApi = new OpenAPIV3Parser().readContents(IOUtils.toString(openApiReader), null, null)
                .getOpenAPI();
        } catch (IOException e) {
            throw new RuntimeException("OpenAPI source can not be parsed", e);
        }
        if (openApi == null)
            throw new IllegalArgumentException("OpenAPI source is in a wrong format");

        return new Builder(openApi);
    }

    /**
     * Creates a OpenApi2MarkupConverter.Builder from a given Swagger YAML or JSON reader.
     *
     * @param openApiReader the Swagger YAML or JSON reader.
     * @return a OpenApi2MarkupConverter Builder
     */
    public static Builder fromSwagger(Reader swaggerReader) {
        Validate.notNull(swaggerReader, "openApiReader must not be null");
        try {
          return new BuilderV2(IOUtils.toString(swaggerReader));
        } catch (IOException e) {
          throw new RuntimeException("OpenAPI source can not be parsed", e);
        }
    }

    /**
     * Returns the global Context
     *
     * @return the global Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Converts the OpenAPI specification into the given {@code outputDirectory}.
     *
     * @param outputDirectory the output directory path
     */
    public void toFolder(Path outputDirectory) {
        Validate.notNull(outputDirectory, "outputDirectory must not be null");

        context.setOutputPath(outputDirectory);

        applyOverviewDocument()
                .writeToFile(outputDirectory.resolve(context.config.getOverviewDocument()), StandardCharsets.UTF_8);
        applyPathsDocument()
                .writeToFile(outputDirectory.resolve(context.config.getPathsDocument()), StandardCharsets.UTF_8);
        applyDefinitionsDocument()
                .writeToFile(outputDirectory.resolve(context.config.getDefinitionsDocument()), StandardCharsets.UTF_8);
        applySecurityDocument()
                .writeToFile(outputDirectory.resolve(context.config.getSecurityDocument()), StandardCharsets.UTF_8);
    }

    private MarkupDocBuilder applyOverviewDocument() {
        return overviewDocument.apply(
                context.createMarkupDocBuilder(),
                OverviewDocument.parameters(context.getOpenApi()));
    }

    private MarkupDocBuilder applyPathsDocument() {
        return pathsDocument.apply(
                context.createMarkupDocBuilder(),
                PathsDocument.parameters(context.getOpenApi().getPaths()));
    }

    private MarkupDocBuilder applyDefinitionsDocument() {
        return definitionsDocument.apply(
                context.createMarkupDocBuilder(),
                DefinitionsDocument.parameters(ModelUtils.getComponentModels(context)));
    }

    private MarkupDocBuilder applySecurityDocument() {
        return securityDocument.apply(
                context.createMarkupDocBuilder(),
                SecurityDocument.parameters(context.getOpenApi().getComponents().getSecuritySchemes()));
    }

    /**
     * Converts the OpenAPI specification into the {@code outputPath} which can be either a directory (e.g /tmp) or a file without extension (e.g /tmp/openApi).
     * Internally the method invokes either {@code toFolder} or {@code toFile}. If the {@code outputPath} is a directory, the directory must exist.
     * Otherwise it cannot be determined if the {@code outputPath} is a directory or not.
     *
     * @param outputPath the output path
     */
    public void toPath(Path outputPath) {
        Validate.notNull(outputPath, "outputPath must not be null");
        if (Files.isDirectory(outputPath)) {
            toFolder(outputPath);
        } else {
            toFile(outputPath);
        }
    }

    /**
     * Converts the OpenAPI specification the given {@code outputFile}.<br>
     * An extension identifying the markup language will be automatically added to file name.
     *
     * @param outputFile the output file
     */
    public void toFile(Path outputFile) {
        Validate.notNull(outputFile, "outputFile must not be null");

        applyOverviewDocument().writeToFile(outputFile, StandardCharsets.UTF_8);
        applyPathsDocument().writeToFile(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        applyDefinitionsDocument().writeToFile(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        applySecurityDocument().writeToFile(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /**
     * Converts the OpenAPI specification the given {@code outputFile}.
     *
     * @param outputFile the output file
     */
    public void toFileWithoutExtension(Path outputFile) {
        Validate.notNull(outputFile, "outputFile must not be null");

        applyOverviewDocument().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8);
        applyPathsDocument().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        applyDefinitionsDocument().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        applySecurityDocument().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /**
     * Builds the document returns it as a String.
     *
     * @return the document as a String
     */
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(applyOverviewDocument().toString());
        sb.append(applyPathsDocument().toString());
        sb.append(applyDefinitionsDocument().toString());
        sb.append(applySecurityDocument().toString());
        return sb.toString();
    }

    public static class BuilderV2 extends Builder {

      /**
       * Creates a Swagger V2 Builder from a remote URL.
       *
       * @param swaggerUrl the remote URL
       */
      BuilderV2(URL swaggerUrl) {
        super(readAndConvertSwagger(swaggerUrl));
      }

      private static OpenAPI readAndConvertSwagger(URL swaggerUrl) {
        try {
          String swaggerLocation = swaggerUrl.toURI().toString();
          return readAndConvertSwagger(swaggerLocation);
        } catch (URISyntaxException e) {
          throw new IllegalArgumentException("swaggerUrl is in a wrong format", e);
        }
      }

      private static OpenAPI readAndConvertSwagger(String url) {
        SwaggerConverter v2Converter = new SwaggerConverter();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = v2Converter.readLocation(url, null, options);
        return result.getOpenAPI();
      }

      private static OpenAPI convertSwaggerFromString(String swaggerString) {
        SwaggerConverter v2Converter = new SwaggerConverter();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = v2Converter.readContents(swaggerString, null, options);
        return result.getOpenAPI();
      }

      /**
       * Creates a Swagger V2 Builder from a local Path.
       *
       * @param swaggerPath the local Path
       */
      BuilderV2(Path swaggerPath) {
        super(readAndConvertSwagger(swaggerPath.toAbsolutePath().toUri().toString()));
        super.openApiLocation = swaggerPath.toAbsolutePath().toUri();
      }

      public BuilderV2(String swaggerString) {
        super(convertSwaggerFromString(swaggerString));
      }
    }

    public static class Builder {
        protected final OpenAPI openApi;
        protected URI openApiLocation;
        private OpenApi2MarkupConfig config;
        private OpenApi2MarkupExtensionRegistry extensionRegistry;

        /**
         * Creates a Builder from a remote URL.
         *
         * @param openApiUrl the remote URL
         */
        Builder(URL openApiUrl) {
            try {
                this.openApiLocation = openApiUrl.toURI();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("openApiUrl is in a wrong format", e);
            }
            this.openApi = readOpenAPI(openApiUrl.toString());
        }

        /**
         * Creates a Builder from a local Path.
         *
         * @param openApiPath the local Path
         */
        Builder(Path openApiPath) {
            this.openApiLocation = openApiPath.toAbsolutePath().toUri();
            this.openApi = readOpenAPI(openApiPath.toString());
        }

        /**
         * Creates a Builder using a given OpenAPI model.
         *
         * @param openApi the OpenAPI source.
         */
        Builder(OpenAPI openApi) {
            this.openApi = openApi;
            this.openApiLocation = null;
        }

        /**
         * Uses the OpenAPIParser to read the OpenAPI source.
         *
         * @param openApiLocation the location of the OpenAPI source
         * @return the OpenAPI model
         */
        private OpenAPI readOpenAPI(String openApiLocation) {
            OpenAPI openApi = new OpenAPIV3Parser().read(openApiLocation);
            if (openApi == null) {
                throw new IllegalArgumentException("Failed to read the OpenAPI source");
            }
            return openApi;
        }

        public Builder withConfig(OpenApi2MarkupConfig config) {
            Validate.notNull(config, "config must not be null");
            this.config = config;
            return this;
        }

        public Builder withExtensionRegistry(OpenApi2MarkupExtensionRegistry registry) {
            Validate.notNull(registry, "registry must not be null");
            this.extensionRegistry = registry;
            return this;
        }

        public OpenApi2MarkupConverter build() {
            if (config == null)
                config = new OpenApi2MarkupConfigBuilder().build();

            // force anything read from Swagger to V2 overview with UriScheme
            if (this instanceof BuilderV2 && config.getOpenApiVersion() != 2) {
              try {
                FieldUtils.writeField(config, "openApiVersion", 2, true);
              } catch (IllegalAccessException | SecurityException e) {
                LOG.error("Error forcing OpenAPI version to 2", e);
              }
            }

            if (extensionRegistry == null)
                extensionRegistry = new OpenApi2MarkupExtensionRegistryBuilder().build();

            Context context = new Context(config, extensionRegistry, openApi, openApiLocation);

            initExtensions(context);

            applyOpenAPIExtensions(context);

            return new OpenApi2MarkupConverter(context);
        }

        private void initExtensions(Context context) {
            extensionRegistry.getOpenApiModelExtensions().forEach(extension -> extension.setGlobalContext(context));
            extensionRegistry.getOverviewDocumentExtensions().forEach(extension -> extension.setGlobalContext(context));
            extensionRegistry.getDefinitionsDocumentExtensions().forEach(extension -> extension.setGlobalContext(context));
            extensionRegistry.getPathsDocumentExtensions().forEach(extension -> extension.setGlobalContext(context));
            extensionRegistry.getSecurityDocumentExtensions().forEach(extension -> extension.setGlobalContext(context));
        }

        private void applyOpenAPIExtensions(Context context) {
            extensionRegistry.getOpenApiModelExtensions().forEach(extension -> extension.apply(context.getOpenApi()));
        }
    }

    public static class Context {
        private final OpenApi2MarkupConfig config;
        private final OpenAPI openApi;
        private final URI openApiLocation;
        private final OpenApi2MarkupExtensionRegistry extensionRegistry;
        private final Labels labels;
        private Path outputPath;

        public Context(OpenApi2MarkupConfig config,
                       OpenApi2MarkupExtensionRegistry extensionRegistry,
                       OpenAPI openApi,
                       URI openApiLocation) {
            this.config = config;
            this.extensionRegistry = extensionRegistry;
            this.openApi = openApi;
            this.openApiLocation = openApiLocation;
            this.labels = new Labels(config);
        }

        public OpenApi2MarkupConfig getConfig() {
            return config;
        }

        public OpenAPI getOpenApi() {
            return openApi;
        }

        public URI getOpenApiLocation() {
            return openApiLocation;
        }

        public OpenApi2MarkupExtensionRegistry getExtensionRegistry() {
            return extensionRegistry;
        }

        public Labels getLabels() {
            return labels;
        }

        public MarkupDocBuilder createMarkupDocBuilder() {
            return MarkupDocBuilders.documentBuilder(config.getMarkupLanguage(),
                    config.getLineSeparator(), config.getAsciidocPegdownTimeoutMillis()).withAnchorPrefix(config.getAnchorPrefix());
        }

        public Path getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(Path outputPath) {
            this.outputPath = outputPath;
        }
    }
}
