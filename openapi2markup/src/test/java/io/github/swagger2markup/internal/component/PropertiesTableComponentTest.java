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
package io.github.swagger2markup.internal.component;

import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.assertions.DiffUtils;
import io.github.swagger2markup.internal.resolver.DefinitionDocumentResolverFromDefinition;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.utils.ModelUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.Model;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class PropertiesTableComponentTest extends AbstractComponentTest {

    private static final String COMPONENT_NAME = "properties_table";
    private Path outputDirectory;

    @BeforeEach
    public void setUp() {
        outputDirectory = getOutputFile(COMPONENT_NAME);
        FileUtils.deleteQuietly(outputDirectory.toFile());
    }

    @Test
    public void testPropertiesTableComponent() throws URISyntaxException {
        //Given
        Path file = Paths.get(PropertiesTableComponentTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        OpenApi2MarkupConverter converter = OpenApi2MarkupConverter.from(file).build();
        OpenAPI swagger = converter.getContext().getOpenApi();

        Model petModel = ModelUtils.convertToModel(swagger.getComponents().getSchemas().get("Pet"));

        OpenApi2MarkupConverter.Context context = converter.getContext();
        MarkupDocBuilder markupDocBuilder = context.createMarkupDocBuilder();

        List<ObjectType> localDefinitions = new ArrayList<>();
        //When
        markupDocBuilder = new PropertiesTableComponent(context,
                new DefinitionDocumentResolverFromDefinition(context))
                .apply(markupDocBuilder, PropertiesTableComponent.parameters(petModel.getProperties(),
                        "Pet", localDefinitions));
        markupDocBuilder.writeToFileWithoutExtension(outputDirectory, StandardCharsets.UTF_8);

        //Then
        Path expectedFile = getExpectedFile(COMPONENT_NAME);
        DiffUtils.assertThatFileIsEqual(expectedFile, outputDirectory, getReportName(COMPONENT_NAME));

    }
}
