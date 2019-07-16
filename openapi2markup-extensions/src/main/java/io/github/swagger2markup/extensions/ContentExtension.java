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

import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.spi.ContentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ContentExtension {

    private static final Logger logger = LoggerFactory.getLogger(ContentExtension.class);

    protected final OpenApi2MarkupConverter.Context globalContext;
    protected final ContentContext contentContext;


    ContentExtension(OpenApi2MarkupConverter.Context globalContext, ContentContext contentContext) {
        this.globalContext = globalContext;
        this.contentContext = contentContext;
    }

    /**
     * Import contents from a file
     *
     * @param contentPath content file path
     * @param contentConsumer the consumer of the file content
     *
     */
    protected void importContent(Path contentPath, Consumer<Reader> contentConsumer) {

        if (Files.isReadable(contentPath)) {
            try (Reader contentReader = Files.newBufferedReader(contentPath, StandardCharsets.UTF_8)){
                contentConsumer.accept(contentReader);
                if (logger.isInfoEnabled()) {
                    logger.info("Content file {} imported", contentPath);
                }
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to read content file {} > {}", contentPath, e.getMessage());
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to read content file {}", contentPath);
            }
        }
    }

    /**
     * Import content from an Uri
     *
     * @param contentUri content file URI
     * @param contentConsumer the consumer of the file content
     */
    protected void importContent(URI contentUri, Consumer<Reader> contentConsumer) {
        try (Reader contentReader = io.github.swagger2markup.utils.IOUtils.uriReader(contentUri)){
            contentConsumer.accept(contentReader);
            if (logger.isInfoEnabled()) {
                logger.info("Content URI {} processed", contentUri);
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to read content URI {} > {}", contentUri, e.getMessage());
            }
        }
    }
}
