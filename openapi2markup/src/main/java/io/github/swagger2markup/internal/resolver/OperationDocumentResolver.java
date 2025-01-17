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
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.PathOperation;
import io.vavr.Function1;

/**
 * A functor to return the document part of an inter-document cross-references, depending on the context.
 */
public abstract class OperationDocumentResolver implements Function1<PathOperation, String> {

    OpenApi2MarkupConverter.Context context;
    MarkupDocBuilder markupDocBuilder;
    OpenApi2MarkupConfig config;

    public OperationDocumentResolver(OpenApi2MarkupConverter.Context context) {
        this.context = context;
        this.markupDocBuilder = context.createMarkupDocBuilder();
        this.config = context.getConfig();
    }
}