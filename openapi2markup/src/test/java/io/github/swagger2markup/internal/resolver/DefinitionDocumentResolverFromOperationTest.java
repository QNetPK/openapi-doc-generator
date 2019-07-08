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
package io.github.swagger2markup.internal.resolver;

import io.github.swagger2markup.OpenApi2MarkupConfig;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.nio.file.Paths;

import static io.github.swagger2markup.helper.ContextUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;

public class DefinitionDocumentResolverFromOperationTest {

    private final String fileSeparator = FileSystems.getDefault().getSeparator();

    @Test
    public void testDefault() {
        OpenApi2MarkupConverter.Context context = createContext();

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName")).isNull();
    }

    @Test
    public void testWithSeparatedDefinitions() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withSeparatedDefinitions()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName")).isNull();
    }

    @Test
    public void testWithSeparatedOperationsAndInterDocumentCrossReferences() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withSeparatedOperations()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName"))
                .isEqualTo(".." + fileSeparator + "definitions.adoc");
    }

    @Test
    public void testWithSeparatedOperationsAndSeparatedDefinitionsAndInterDocumentCrossReferences() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withSeparatedOperations()
                .withSeparatedDefinitions()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName"))
                .isEqualTo(".." + fileSeparator + "definitions" + fileSeparator + "DefinitionName.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferences() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName"))
                .isEqualTo("definitions.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferencesAndNoOutputPath() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName"))
                .isNull();
    }

    @Test
    public void testWithInterDocumentCrossReferencesAndPrefix() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences("prefix_")
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName"))
                .isEqualTo("prefix_definitions.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferencesAndMarkdown() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new DefinitionDocumentResolverFromOperation(context).apply("DefinitionName"))
                .isEqualTo("definitions.md");
    }
}
