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

package io.github.swagger2markup.spi;

import io.github.swagger2markup.Labels;
import io.github.swagger2markup.OpenApi2MarkupConfig;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.OpenApi2MarkupExtensionRegistry;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.vavr.Function2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MarkupComponent<T> implements Function2<MarkupDocBuilder, T, MarkupDocBuilder> {

    protected static final String COLON = " : ";
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected OpenApi2MarkupConverter.Context context;
    protected Labels labels;
    protected OpenApi2MarkupConfig config;
    protected OpenApi2MarkupExtensionRegistry extensionRegistry;

    public MarkupComponent(OpenApi2MarkupConverter.Context context) {
        this.context = context;
        this.config = context.getConfig();
        this.extensionRegistry = context.getExtensionRegistry();
        this.labels = context.getLabels();
    }
}
