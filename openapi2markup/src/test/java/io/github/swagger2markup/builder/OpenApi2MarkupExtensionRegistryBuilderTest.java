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
package io.github.swagger2markup.builder;

import io.github.swagger2markup.OpenApi2MarkupExtensionRegistry;
import io.github.swagger2markup.spi.OpenApiModelExtension;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApi2MarkupExtensionRegistryBuilderTest {

    @Test
    public void testRegistering() {
        OpenApi2MarkupExtensionRegistryBuilder registryBuilder = new OpenApi2MarkupExtensionRegistryBuilder();
        registryBuilder.withOpenApiModelExtension(new MyOpenApiModelExtension());
    }

    @Test
    public void testListing() {
        OpenApiModelExtension ext1 = new MyOpenApiModelExtension();
        OpenApiModelExtension ext2 = new MyOpenApiModelExtension();
        OpenApiModelExtension ext3 = new OpenApiModelExtension() {
            public void apply(OpenAPI swagger) {
            }
        };

        OpenApi2MarkupExtensionRegistry registry = new OpenApi2MarkupExtensionRegistryBuilder()
                .withOpenApiModelExtension(ext2)
                .withOpenApiModelExtension(ext3)
                .withOpenApiModelExtension(ext1)
                .build();
        List<OpenApiModelExtension> extensions = registry.getOpenApiModelExtensions();
        assertThat(extensions.size()).isEqualTo(3);
        assertThat(extensions).contains(ext1, ext2, ext3);
    }
}
