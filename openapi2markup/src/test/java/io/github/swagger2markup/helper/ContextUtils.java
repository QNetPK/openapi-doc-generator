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
package io.github.swagger2markup.helper;

import io.github.swagger2markup.OpenApi2MarkupConfig;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.OpenApi2MarkupExtensionRegistry;
import io.github.swagger2markup.builder.OpenApi2MarkupConfigBuilder;
import io.github.swagger2markup.builder.OpenApi2MarkupExtensionRegistryBuilder;
import io.swagger.v3.oas.models.OpenAPI;

public class ContextUtils {

    public static OpenApi2MarkupConverter.Context createContext() {
        OpenApi2MarkupConfig config = new OpenApi2MarkupConfigBuilder().build();
        OpenApi2MarkupExtensionRegistry extensionRegistry = new OpenApi2MarkupExtensionRegistryBuilder().build();
        return new OpenApi2MarkupConverter.Context(config, extensionRegistry, null, null);
    }

    public static OpenApi2MarkupConverter.Context createContext(OpenApi2MarkupConfig config) {
        OpenApi2MarkupExtensionRegistry extensionRegistry = new OpenApi2MarkupExtensionRegistryBuilder().build();
        return new OpenApi2MarkupConverter.Context(config, extensionRegistry, null, null);
    }

    public static OpenApi2MarkupConverter.Context createContext(OpenApi2MarkupConfig config, OpenAPI swagger) {
        OpenApi2MarkupExtensionRegistry extensionRegistry = new OpenApi2MarkupExtensionRegistryBuilder().build();
        return new OpenApi2MarkupConverter.Context(config, extensionRegistry, swagger, null);
    }
}
