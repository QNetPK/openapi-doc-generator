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
package io.github.swagger2markup.extensions;

import io.github.swagger2markup.OpenApi2MarkupConfig;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.OpenApi2MarkupExtensionRegistry;
import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.builder.OpenApi2MarkupExtensionRegistryBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaExtensionTest {



    @Test
    public void testOpenApi2AsciiDocSchemaExtension() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(SchemaExtensionTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/test/asciidoc/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Properties properties = new Properties();
        properties.load(SchemaExtensionTest.class.getResourceAsStream("/config/config.properties"));
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder(properties)
                .build();
        OpenApi2MarkupExtensionRegistry registry = new OpenApi2MarkupExtensionRegistryBuilder()
                //.withDefinitionsDocumentExtension(new SchemaExtension(Paths.get("src/test/resources/docs/asciidoc/extensions").toUri()))
                .build();
        OpenApi2MarkupConverter.from(file)
                .withConfig(config)
                .withExtensionRegistry(registry)
                .build()
                .toFolder(outputDirectory);

        //Then
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("definitions.adoc")))).contains(
                "=== Pet");
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("definitions.adoc")))).contains(
                "==== XML Schema");
    }
}
