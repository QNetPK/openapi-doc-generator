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
import io.github.swagger2markup.model.PathOperation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.nio.file.Paths;

import static io.github.swagger2markup.helper.ContextUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;

public class OperationDocumentNameResolverTest {

    private final String fileSeparator = FileSystems.getDefault().getSeparator();

    private PathOperation operation;

    @BeforeEach
    public void setUp() {
        operation = new PathOperation(HttpMethod.GET, "/test", new Operation());
    }

    @Test
    public void testDefault() {
        OpenApi2MarkupConverter.Context context = createContext();

        assertThat(new OperationDocumentNameResolver(context).apply(operation)).isEqualTo("paths.adoc");
    }

    @Test
    public void testWithSeparatedOperations() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withSeparatedOperations()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);

        assertThat(new OperationDocumentNameResolver(context).apply(operation))
                .isEqualTo("operations" + fileSeparator + "test_get.adoc");
    }

    @Test
    public void testWithSeparatedOperationsAndInterDocumentCrossReferences() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withSeparatedOperations()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);

        assertThat(new OperationDocumentNameResolver(context).apply(operation))
                .isEqualTo("operations" + fileSeparator + "test_get.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferencesAndNoOutputPath() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);

        assertThat(new OperationDocumentNameResolver(context).apply(operation))
                .isEqualTo("paths.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferences() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences()
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new OperationDocumentNameResolver(context).apply(operation))
                .isEqualTo("paths.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferencesAndPrefix() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences("prefix_")
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new OperationDocumentNameResolver(context).apply(new PathOperation(HttpMethod.GET, "/test", new Operation())))
                .isEqualTo("paths.adoc");
    }

    @Test
    public void testWithInterDocumentCrossReferencesAndMarkdown() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder()
                .withInterDocumentCrossReferences()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        OpenApi2MarkupConverter.Context context = createContext(config);
        context.setOutputPath(Paths.get("/tmp"));

        assertThat(new OperationDocumentNameResolver(context).apply(operation))
                .isEqualTo("paths.md");
    }
}
