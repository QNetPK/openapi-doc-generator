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

import io.github.swagger2markup.builder.MyExtension;
import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.builder.OpenApi2MarkupExtensionRegistryBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DocumentationTest {

    public void localOpenAPISpec() throws URISyntaxException, IOException {
        // tag::localOpenAPISpec[]
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");
        Path outputDirectory = Paths.get("build/asciidoc");

        OpenApi2MarkupConverter.from(localOpenAPIFile) // <1>
                .build() // <2>
                .toFolder(outputDirectory); // <3>
        // end::localOpenAPISpec[]
    }

    public void remoteOpenAPISpec() throws URISyntaxException, IOException {
        // tag::remoteOpenAPISpec[]
        URL remoteOpenAPIFile = new URL("http://petstore.swagger.io/v2/swagger.json");
        Path outputDirectory = Paths.get("build/asciidoc");

        OpenApi2MarkupConverter.from(remoteOpenAPIFile) // <1>
                .build() // <2>
                .toFolder(outputDirectory); // <3>
        // end::remoteOpenAPISpec[]
    }

    public void convertIntoOneFile() throws URISyntaxException, IOException {

        // tag::convertIntoOneFile[]
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");
        Path outputFile = Paths.get("build/swagger.adoc");

        OpenApi2MarkupConverter.from(localOpenAPIFile)
                .build()
                .toFile(outputFile);
        // end::convertIntoOneFile[]
    }


    public void convertIntoString() throws URISyntaxException, IOException {

        // tag::convertIntoString[]
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");

        String documentation = OpenApi2MarkupConverter.from(localOpenAPIFile)
                .build()
                .toString();
        // end::convertIntoString[]
    }

    public void swagger2MarkupConfigBuilder() {
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");

        // tag::swagger2MarkupConfigBuilder[]
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder() //<1>
                .withMarkupLanguage(MarkupLanguage.MARKDOWN) //<2>
                .withOutputLanguage(Language.DE) //<3>
                .withPathsGroupedBy(GroupBy.TAGS) //<4>
                .build(); //<5>

        OpenApi2MarkupConverter converter = OpenApi2MarkupConverter.from(localOpenAPIFile)
                .withConfig(config) //<6>
                .build();
        // end::swagger2MarkupConfigBuilder[]
    }

    public void swagger2MarkupConfigFromProperties() throws IOException {
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");

        // tag::swagger2MarkupConfigFromProperties[]
        Properties properties = new Properties();
        properties.load(DocumentationTest.class.getResourceAsStream("/config/config.properties")); //<1>

        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder(properties) //<2>
                .build();

        OpenApi2MarkupConverter converter = OpenApi2MarkupConverter.from(localOpenAPIFile)
                .withConfig(config)
                .build();
        // end::swagger2MarkupConfigFromProperties[]
    }

    public void swagger2MarkupConfigFromMap() throws IOException {
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");

        // tag::swagger2MarkupConfigFromMap[]
        Map<String, String> configMap = new HashMap<>(); //<1>
        configMap.put(OpenApi2MarkupProperties.MARKUP_LANGUAGE, MarkupLanguage.MARKDOWN.toString());
        configMap.put(OpenApi2MarkupProperties.OUTPUT_LANGUAGE, Language.DE.toString());
        configMap.put(OpenApi2MarkupProperties.PATHS_GROUPED_BY, GroupBy.TAGS.toString());

        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder(configMap) //<2>
                .build();

        OpenApi2MarkupConverter converter = OpenApi2MarkupConverter.from(localOpenAPIFile)
                .withConfig(config)
                .build();
        // end::swagger2MarkupConfigFromMap[]
    }

    public void swagger2MarkupExtensionRegistryBuilder() throws IOException, ConfigurationException {
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");

        // tag::swagger2MarkupExtensionRegistryBuilder[]

        OpenApi2MarkupExtensionRegistry registry = new OpenApi2MarkupExtensionRegistryBuilder() //<1>
                .withDefinitionsDocumentExtension(new MyExtension()) //<2>
                .build(); //<3>

        OpenApi2MarkupConverter converter = OpenApi2MarkupConverter.from(localOpenAPIFile)
                .withExtensionRegistry(registry) //<4>
                .build();
        // end::swagger2MarkupExtensionRegistryBuilder[]
    }

    public void swagger2MarkupConfigFromCommonsConfiguration() throws IOException, ConfigurationException {
        Path localOpenAPIFile = Paths.get("/path/to/swagger.yaml");

        // tag::swagger2MarkupConfigFromCommonsConfiguration[]
        Configurations configs = new Configurations();
        Configuration configuration = configs.properties("config.properties"); //<1>

        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder(configuration) //<2>
                .build();

        OpenApi2MarkupConverter converter = OpenApi2MarkupConverter.from(localOpenAPIFile)
                .withConfig(config)
                .build();
        // end::swagger2MarkupConfigFromCommonsConfiguration[]
    }
}