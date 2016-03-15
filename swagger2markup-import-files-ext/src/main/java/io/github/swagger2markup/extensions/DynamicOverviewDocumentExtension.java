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

import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.spi.OverviewDocumentExtension;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dynamically search for markup files in {@code contentPath} to append to Overview, with the format :<br>
 * - {@code document-before-*.<markup.ext>} : import before Overview document with levelOffset = 0<br>
 * - {@code document-begin-*.<markup.ext>} : import just after Overview document main title with levelOffset = 1<br>
 * - {@code document-end-*.<markup.ext>} : import at the end of Overview document with levelOffset = 1<br>
 * <p>
 * Markup files are appended in the natural order of their names, for each category.
 */
public final class DynamicOverviewDocumentExtension extends OverviewDocumentExtension {

    private static final Logger logger = LoggerFactory.getLogger(DynamicOverviewDocumentExtension.class);

    protected Path contentPath;

    public DynamicOverviewDocumentExtension() {
        super();
    }

    public DynamicOverviewDocumentExtension(Path contentPath) {
        super();

        Validate.notNull(contentPath);
        this.contentPath = contentPath;
    }

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        if (contentPath == null) {
            if (globalContext.getSwaggerLocation() == null || !globalContext.getSwaggerLocation().getScheme().equals("file")) {
                if (logger.isWarnEnabled())
                    logger.warn("Disable > DynamicOverviewContentExtension > Can't set default contentPath from swaggerLocation. You have to explicitly configure the content path.");
            } else {
                contentPath = Paths.get(globalContext.getSwaggerLocation()).getParent();
            }
        }
    }

    @Override
    public void apply(Context context) {
        Validate.notNull(context);

        if (contentPath != null) {
            DynamicContentExtension dynamicContent = new DynamicContentExtension(globalContext, context);
            OverviewDocumentExtension.Position position = context.getPosition();
            switch (position) {
                case DOCUMENT_BEFORE:
                    dynamicContent.extensionsSection(contentPath, contentPrefix(position), levelOffset(context));
                    break;
                case DOCUMENT_BEGIN:
                    dynamicContent.extensionsSection(contentPath, contentPrefix(position), levelOffset(context));
                    break;
                case DOCUMENT_END:
                    dynamicContent.extensionsSection(contentPath, contentPrefix(position), levelOffset(context));
                    break;
                default:
                    throw new RuntimeException(String.format("Unknown position '%s'", position));
            }
        }
    }

    private String contentPrefix(Position position) {
        return position.name().toLowerCase().replace('_', '-');
    }

}
