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
import io.github.swagger2markup.assertions.DiffUtils;
import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.builder.OpenApi2MarkupExtensionRegistryBuilder;
import io.github.swagger2markup.markup.builder.LineSeparator;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;

public class SpringRestDocsExtensionsTest {

    private static final Logger LOG = LoggerFactory.getLogger(SpringRestDocsExtensionsTest.class);
    private static final String[] EXPECTED_FILES = new String[]{"definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc"};
    private List<String> expectedFiles;

    @BeforeEach
    public void setUp(){
        expectedFiles = new ArrayList<>(asList(EXPECTED_FILES));
    }

    @Test
    public void testOpenApi2AsciiDocConversionWithSpringRestDocsExtension() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(SpringRestDocsExtensionsTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/test/asciidoc/spring_rest_docs");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        OpenApi2MarkupExtensionRegistry registry = new OpenApi2MarkupExtensionRegistryBuilder()
                //.withPathsDocumentExtension(new SpringRestDocsExtension(Paths.get("src/test/resources/docs/asciidoc/paths").toUri()).withDefaultSnippets())
                .build();

        Properties properties = new Properties();
        properties.load(SpringRestDocsExtensionsTest.class.getResourceAsStream("/config/config.properties"));
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder(properties)
                .withLineSeparator(LineSeparator.UNIX)
                .build();

        OpenApi2MarkupConverter.fromSwagger(file)
                .withConfig(config)
                .withExtensionRegistry(registry)
                .build()
                .toFolder(outputDirectory);

        //Then
        String[] files = outputDirectory.toFile().list();
        Assertions.assertThat(files).hasSize(4).containsAll(expectedFiles);

        Path expectedFilesDirectory = Paths.get(SpringRestDocsExtensionsTest.class.getResource("/expected_results/asciidoc/spring_rest_docs").toURI());
        DiffUtils.assertThatAllFilesAreEqual(outputDirectory, expectedFilesDirectory, "testOpenApi2AsciiDocConversionWithSpringRestDocsExtension.html");
    }
}
