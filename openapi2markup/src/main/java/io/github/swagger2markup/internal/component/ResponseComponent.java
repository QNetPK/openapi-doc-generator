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


import ch.netzwerg.paleo.StringColumn;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.internal.adapter.PropertyAdapter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.BasicType;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.Type;
import io.github.swagger2markup.internal.utils.ModelUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.Model;
import io.github.swagger2markup.model.PathOperation;
import io.github.swagger2markup.spi.MarkupComponent;
import io.github.swagger2markup.spi.PathsDocumentExtension;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static ch.netzwerg.paleo.ColumnIds.StringColumnId;
import static io.github.swagger2markup.Labels.*;
import static io.github.swagger2markup.internal.utils.InlineSchemaUtils.createInlineType;
import static io.github.swagger2markup.internal.utils.MapUtils.toSortedMap;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ResponseComponent extends MarkupComponent<ResponseComponent.Parameters> {

    private final TableComponent tableComponent;
    private final Map<String, Model> definitions;
    private final DocumentResolver definitionDocumentResolver;

    ResponseComponent(OpenApi2MarkupConverter.Context context,
                      DocumentResolver definitionDocumentResolver) {
        super(context);
        this.definitions = ModelUtils.getComponentModels(context);
        this.definitionDocumentResolver = Validate.notNull(definitionDocumentResolver, "DocumentResolver must not be null");
        this.tableComponent = new TableComponent(context);
    }

    public static ResponseComponent.Parameters parameters(PathOperation operation,
                                                          int titleLevel,
                                                          List<ObjectType> inlineDefinitions) {
        return new ResponseComponent.Parameters(operation, titleLevel, inlineDefinitions);
    }

    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, Parameters params) {
        PathOperation operation = params.operation;
        Map<String, ApiResponse> responses = operation.getOperation().getResponses();

        MarkupDocBuilder responsesBuilder = copyMarkupDocBuilder(markupDocBuilder);
        applyPathsDocumentExtension(new PathsDocumentExtension.Context(PathsDocumentExtension.Position.OPERATION_RESPONSES_BEGIN, responsesBuilder, operation));
        if (MapUtils.isNotEmpty(responses)) {
            StringColumn.Builder httpCodeColumnBuilder = StringColumn.builder(StringColumnId.of(labels.getLabel(HTTP_CODE_COLUMN)))
                    .putMetaData(TableComponent.WIDTH_RATIO, "2");
            StringColumn.Builder descriptionColumnBuilder = StringColumn.builder(StringColumnId.of(labels.getLabel(DESCRIPTION_COLUMN)))
                    .putMetaData(TableComponent.WIDTH_RATIO, "10")
                    .putMetaData(TableComponent.HEADER_COLUMN, "true");
            StringColumn.Builder mediaColumnBuilder = StringColumn.builder(StringColumnId.of(labels.getLabel(MEDIA_TYPE_COLUMN)))
                .putMetaData(TableComponent.WIDTH_RATIO, "4")
                .putMetaData(TableComponent.HEADER_COLUMN, "true");
            StringColumn.Builder schemaColumnBuilder = StringColumn.builder(StringColumnId.of(labels.getLabel(SCHEMA_COLUMN)))
                    .putMetaData(TableComponent.WIDTH_RATIO, "4")
                    .putMetaData(TableComponent.HEADER_COLUMN, "true");

            Map<String, ApiResponse> sortedResponses = toSortedMap(responses, config.getResponseOrdering());
            sortedResponses.forEach((String responseName, ApiResponse response) -> {
              if (response.getContent() == null || response.getContent().isEmpty()) {
                response.setContent(createEmptyContent());
              }
              for (Entry<String, MediaType> mType : response.getContent().entrySet()) {
                String schemaContent = labels.getLabel(NO_CONTENT);

                Model model = ModelUtils.convertToModel(mType.getValue().getSchema());
                Type type = null;

                if (model != null) {
                    type = ModelUtils.getType(model, definitions, definitionDocumentResolver);
                } else {
                    type = new BasicType("string", responseName);
                }

                if (config.isInlineSchemaEnabled()) {
                    type = createInlineType(type, labels.getLabel(RESPONSE) + " " + responseName, operation.getId() + " " + labels.getLabel(RESPONSE) + " " + responseName, params.inlineDefinitions);
                }

                schemaContent = type.displaySchema(markupDocBuilder);


                MarkupDocBuilder descriptionBuilder = copyMarkupDocBuilder(markupDocBuilder);

                descriptionBuilder.text(markupDescription(config.getOpenApiMarkupLanguage(), markupDocBuilder, response.getDescription()));

                Map<String, Header> headers = response.getHeaders();
                if (MapUtils.isNotEmpty(headers)) {
                    descriptionBuilder.newLine(true).boldText(labels.getLabel(HEADERS_COLUMN)).text(COLON);
                    for (Map.Entry<String, Header> header : headers.entrySet()) {
                        descriptionBuilder.newLine(true);
                        Header headerProperty = header.getValue();
                        PropertyAdapter headerPropertyAdapter = new PropertyAdapter(headerProperty.getSchema());
                        Type propertyType = headerPropertyAdapter.getType(definitionDocumentResolver);
                        String headerDescription = markupDescription(config.getOpenApiMarkupLanguage(), markupDocBuilder, headerProperty.getDescription());
                        Optional<Object> optionalDefaultValue = headerPropertyAdapter.getDefaultValue();

                        descriptionBuilder
                                .literalText(header.getKey())
                                .text(String.format(" (%s)", propertyType.displaySchema(markupDocBuilder)));

                        if (isNotBlank(headerDescription) || optionalDefaultValue.isPresent()) {
                            descriptionBuilder.text(COLON);

                            if (isNotBlank(headerDescription) && !headerDescription.endsWith("."))
                                headerDescription += ".";

                            descriptionBuilder.text(headerDescription);

                            optionalDefaultValue.ifPresent(o -> descriptionBuilder.text(" ")
                                    .boldText(labels.getLabel(DEFAULT_COLUMN))
                                    .text(COLON).literalText(Json.pretty(o)));
                        }
                    }
                }

                httpCodeColumnBuilder.add(boldText(markupDocBuilder, responseName));
                descriptionColumnBuilder.add(descriptionBuilder.toString());
                mediaColumnBuilder.add(mType.getKey());
                schemaColumnBuilder.add(schemaContent);
              }
            });

            responsesBuilder = tableComponent.apply(responsesBuilder, TableComponent.parameters(httpCodeColumnBuilder.build(),
                    descriptionColumnBuilder.build(),
                    mediaColumnBuilder.build(),
                    schemaColumnBuilder.build()));
        }
        applyPathsDocumentExtension(new PathsDocumentExtension.Context(PathsDocumentExtension.Position.OPERATION_RESPONSES_END, responsesBuilder, operation));
        String responsesContent = responsesBuilder.toString();

        applyPathsDocumentExtension(new PathsDocumentExtension.Context(PathsDocumentExtension.Position.OPERATION_RESPONSES_BEFORE, markupDocBuilder, operation));
        if (isNotBlank(responsesContent)) {
            markupDocBuilder.sectionTitleLevel(params.titleLevel, labels.getLabel(RESPONSES));
            markupDocBuilder.text(responsesContent);
        }
        applyPathsDocumentExtension(new PathsDocumentExtension.Context(PathsDocumentExtension.Position.OPERATION_RESPONSES_AFTER, markupDocBuilder, operation));
        return markupDocBuilder;
    }

    private Content createEmptyContent() {
      Content emptyContent = new Content();
      emptyContent.addMediaType("application/json", new MediaType());
      return emptyContent;
    }

    /**
     * Apply extension context to all OperationsContentExtension.
     *
     * @param context context
     */
    private void applyPathsDocumentExtension(PathsDocumentExtension.Context context) {
        extensionRegistry.getPathsDocumentExtensions().forEach(extension -> extension.apply(context));
    }

    public static class Parameters {
        private final PathOperation operation;
        private final int titleLevel;
        private final List<ObjectType> inlineDefinitions;

        public Parameters(PathOperation operation,
                          int titleLevel,
                          List<ObjectType> inlineDefinitions) {

            this.operation = Validate.notNull(operation, "PathOperation must not be null");
            this.titleLevel = titleLevel;
            this.inlineDefinitions = Validate.notNull(inlineDefinitions, "InlineDefinitions must not be null");
        }
    }
}
