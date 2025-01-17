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

import ch.netzwerg.paleo.ColumnIds;
import ch.netzwerg.paleo.StringColumn;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.internal.adapter.PropertyAdapter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.type.Type;
import io.github.swagger2markup.internal.utils.ModelUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.Model;
import io.github.swagger2markup.spi.MarkupComponent;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.swagger2markup.Labels.*;
import static io.github.swagger2markup.internal.utils.InlineSchemaUtils.createInlineType;
import static io.github.swagger2markup.internal.utils.MapUtils.toSortedMap;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.copyMarkupDocBuilder;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


public class PropertiesTableComponent extends MarkupComponent<PropertiesTableComponent.Parameters> {


    private final DocumentResolver definitionDocumentResolver;
    private final TableComponent tableComponent;
    private final Map<String, Model> definitions;

    /**
     * Build a generic property table
     *
     * @param definitionDocumentResolver definition document resolver to apply to property type cross-reference
     */
    PropertiesTableComponent(OpenApi2MarkupConverter.Context context,
      DocumentResolver definitionDocumentResolver) {
        super(context);
        this.definitionDocumentResolver = definitionDocumentResolver;
        this.tableComponent = new TableComponent(context);
        this.definitions = ModelUtils.getComponentModels(context);
    }

    public static PropertiesTableComponent.Parameters parameters(Map<String, Schema> properties,
      String parameterName,
      List<ObjectType> inlineDefinitions) {
        return new PropertiesTableComponent.Parameters(properties, parameterName, inlineDefinitions);
    }

    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, Parameters params) {
        //TODO: This method is too complex, split it up in smaller methods to increase readability
        StringColumn.Builder nameColumnBuilder = StringColumn.builder(ColumnIds.StringColumnId.of(labels.getLabel(NAME_COLUMN)))
          .putMetaData(TableComponent.WIDTH_RATIO, "3");

        StringColumn.Builder descriptionColumnBuilder = StringColumn.builder(ColumnIds.StringColumnId.of(labels.getLabel(DESCRIPTION_COLUMN)))
          .putMetaData(TableComponent.WIDTH_RATIO, "11")
          .putMetaData(TableComponent.HEADER_COLUMN, "true");

        StringColumn.Builder schemaColumnBuilder = StringColumn.builder(ColumnIds.StringColumnId.of(labels.getLabel(SCHEMA_COLUMN)))
          .putMetaData(TableComponent.WIDTH_RATIO, "4")
          .putMetaData(TableComponent.HEADER_COLUMN, "true");

        Map<String, Schema> properties = params.properties;
        if (MapUtils.isNotEmpty(properties)) {
            Map<String, Schema> sortedProperties = toSortedMap(properties, config.getPropertyOrdering());
            sortedProperties.forEach((String propertyName, Schema property) -> {
                PropertyAdapter propertyAdapter = new PropertyAdapter(property);
                Type propertyType = propertyAdapter.getType(definitionDocumentResolver, definitions);

                if (config.isInlineSchemaEnabled()) {
                    propertyType = createInlineType(propertyType, propertyName, params.parameterName + " " + propertyName, params.inlineDefinitions);
                }

                Optional<Object> optionalExample = propertyAdapter.getExample(config.isGeneratedExamplesEnabled(), markupDocBuilder, definitions);
                Optional<Object> optionalDefaultValue = propertyAdapter.getDefaultValue();
                Optional<Integer> optionalMaxLength = propertyAdapter.getMaxlength();
                Optional<Integer> optionalMinLength = propertyAdapter.getMinlength();
                Optional<String> optionalPattern = propertyAdapter.getPattern();

                Optional<BigDecimal> optionalMinValue = propertyAdapter.getMin();
                boolean exclusiveMin = propertyAdapter.getExclusiveMin();
                Optional<BigDecimal> optionalMaxValue = propertyAdapter.getMax();
                boolean exclusiveMax = propertyAdapter.getExclusiveMax();

                MarkupDocBuilder propertyNameContent = copyMarkupDocBuilder(markupDocBuilder);
                propertyNameContent.boldTextLine(propertyName, true);
                if (Optional.ofNullable(property.getRequired()).isPresent())
                    propertyNameContent.italicText(labels.getLabel(FLAGS_REQUIRED).toLowerCase());
                else
                    propertyNameContent.italicText(labels.getLabel(FLAGS_OPTIONAL).toLowerCase());
                if (propertyAdapter.getReadOnly()) {
                    propertyNameContent.newLine(true);
                    propertyNameContent.italicText(labels.getLabel(FLAGS_READ_ONLY).toLowerCase());
                }

                MarkupDocBuilder descriptionContent = copyMarkupDocBuilder(markupDocBuilder);
                String description = markupDescription(config.getOpenApiMarkupLanguage(), markupDocBuilder, propertyAdapter.getDescription(definitions));
                if (isNotBlank(description))
                    descriptionContent.text(description);

                if (optionalDefaultValue.isPresent()) {
                    if (isNotBlank(descriptionContent.toString())) {
                        descriptionContent.newLine(true);
                    }
                    descriptionContent.boldText(labels.getLabel(DEFAULT_COLUMN)).text(COLON).literalText(Json.pretty(optionalDefaultValue.get()));
                }

                if (optionalMinLength.isPresent() && optionalMaxLength.isPresent()) {
                    // combination of minlength/maxlength
                    Integer minLength = optionalMinLength.get();
                    Integer maxLength = optionalMaxLength.get();

                    if (isNotBlank(descriptionContent.toString())) {
                        descriptionContent.newLine(true);
                    }

                    String lengthRange = minLength + " - " + maxLength;
                    if (minLength.equals(maxLength)) {
                        lengthRange = minLength.toString();
                    }

                    descriptionContent.boldText(labels.getLabel(LENGTH_COLUMN)).text(COLON).literalText(lengthRange);

                } else {
                    if (optionalMinLength.isPresent()) {
                        if (isNotBlank(descriptionContent.toString())) {
                            descriptionContent.newLine(true);
                        }
                        descriptionContent.boldText(labels.getLabel(MINLENGTH_COLUMN)).text(COLON).literalText(optionalMinLength.get().toString());
                    }

                    if (optionalMaxLength.isPresent()) {
                        if (isNotBlank(descriptionContent.toString())) {
                            descriptionContent.newLine(true);
                        }
                        descriptionContent.boldText(labels.getLabel(MAXLENGTH_COLUMN)).text(COLON).literalText(optionalMaxLength.get().toString());
                    }
                }

                if (optionalPattern.isPresent()) {
                    if (isNotBlank(descriptionContent.toString())) {
                        descriptionContent.newLine(true);
                    }
                    descriptionContent.boldText(labels.getLabel(PATTERN_COLUMN)).text(COLON).literalText(Json.pretty(optionalPattern.get()));
                }

                DecimalFormat numberFormatter = new DecimalFormat("#.##",
                  DecimalFormatSymbols.getInstance(config.getOutputLanguage().toLocale()));

                if (optionalMinValue.isPresent()) {
                    if (isNotBlank(descriptionContent.toString())) {
                        descriptionContent.newLine(true);
                    }
                    String minValueColumn = exclusiveMin ? labels.getLabel(MINVALUE_EXCLUSIVE_COLUMN) : labels.getLabel(MINVALUE_COLUMN);
                    descriptionContent.boldText(minValueColumn).text(COLON).literalText(numberFormatter.format(optionalMinValue.get()));
                }

                if (optionalMaxValue.isPresent()) {
                    if (isNotBlank(descriptionContent.toString())) {
                        descriptionContent.newLine(true);
                    }
                    String maxValueColumn = exclusiveMax ? labels.getLabel(MAXVALUE_EXCLUSIVE_COLUMN) : labels.getLabel(MAXVALUE_COLUMN);
                    descriptionContent.boldText(maxValueColumn).text(COLON).literalText(numberFormatter.format(optionalMaxValue.get()));
                }

                if (optionalExample.isPresent()) {
                    if (isNotBlank(descriptionContent.toString())) {
                        descriptionContent.newLine(true);
                    }

                    if(propertyType instanceof RefType) {
                      if (isReferenceLink(optionalExample.get().toString())) {
                        descriptionContent.boldText(labels.getLabel(EXAMPLE_COLUMN)).text(COLON).text(optionalExample.get().toString());
                      } else {
                        descriptionContent.boldText(labels.getLabel(EXAMPLE_COLUMN)).text(COLON).crossReference(optionalExample.get().toString());
                      }
                    } else {
                        descriptionContent.boldText(labels.getLabel(EXAMPLE_COLUMN)).text(COLON).literalText(Json.pretty(optionalExample.get()));
                    }
                }

                nameColumnBuilder.add(propertyNameContent.toString());
                descriptionColumnBuilder.add(descriptionContent.toString());
                schemaColumnBuilder.add(propertyType.displaySchema(markupDocBuilder));
            });
        }

        return tableComponent.apply(markupDocBuilder, TableComponent.parameters(
          nameColumnBuilder.build(),
          descriptionColumnBuilder.build(),
          schemaColumnBuilder.build()));
    }

    /*
     * Check if a string is a link to a reference, format <<_referenceClass>>
      *
     * @param possibleAnchor String to check
     * @return true if the string is a link to an anchor, false otherwise
     */
    private boolean isReferenceLink(String possibleAnchor) {
        return possibleAnchor.startsWith("<<_") && possibleAnchor.endsWith(">>");
    }

    public static class Parameters {
        private final Map<String, Schema> properties;
        private final String parameterName;
        private final List<ObjectType> inlineDefinitions;

        public Parameters(Map<String, Schema> properties,
          String parameterName,
          List<ObjectType> inlineDefinitions) {

            this.properties = Validate.notNull(properties, "Properties must not be null");
            this.parameterName = Validate.notBlank(parameterName, "ParameterName must not be blank");
            this.inlineDefinitions = Validate.notNull(inlineDefinitions, "InlineDefinitions must not be null");
        }
    }
}
