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
package io.github.swagger2markup.internal.adapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.swagger2markup.OpenApi2MarkupConfig;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ArrayType;
import io.github.swagger2markup.internal.type.BasicType;
import io.github.swagger2markup.internal.type.EnumType;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.type.Type;
import io.github.swagger2markup.internal.utils.ExamplesUtil;
import io.github.swagger2markup.internal.utils.InlineSchemaUtils;
import io.github.swagger2markup.internal.utils.ModelUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.BodyParameter;
import io.github.swagger2markup.model.Model;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.boldText;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.literalText;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;

public class ParameterAdapter {

    private final Parameter parameter;
    private final List<ObjectType> inlineDefinitions = new ArrayList<>();
    private final OpenApi2MarkupConfig config;
    private Type type;

    public ParameterAdapter(OpenApi2MarkupConverter.Context context,
                            PathOperation operation,
                            Parameter parameter,
                            DocumentResolver definitionDocumentResolver) {
        Validate.notNull(parameter, "parameter must not be null");
        this.parameter = parameter;
        type = getType(ModelUtils.getComponentModels(context), definitionDocumentResolver);
        config = context.getConfig();
        if (config.isInlineSchemaEnabled()) {
            if (config.isFlatBodyEnabled()) {
                if (!(type instanceof ObjectType)) {
                    type = InlineSchemaUtils.createInlineType(type, parameter.getName(), operation.getId() + " " + parameter.getName(), inlineDefinitions);
                }
            } else {
                type = InlineSchemaUtils.createInlineType(type, parameter.getName(), operation.getId() + " " + parameter.getName(), inlineDefinitions);
            }
        }
    }

    /**
     * Generate a default example value for parameter.
     *
     * @param parameter parameter
     * @return a generated example for the parameter
     */
    public static Object generateExample(Parameter parameter) {
        Schema schema = parameter.getSchema();

        switch (schema.getType()) {
            case "integer":
                return 0;
            case "number":
                return 0.0;
            case "boolean":
                return true;
            case "string":
                return ExamplesUtil.generateStringExample(schema.getFormat(), schema.getEnum());
            default:
                return schema.getType();
        }
    }

    public String getName() {
        return parameter.getName();
    }

    public String getUniqueName() {
        return type.getUniqueName();
    }

    public String displaySchema(MarkupDocBuilder docBuilder) {
        return type.displaySchema(docBuilder);
    }

    public String displayDefaultValue(MarkupDocBuilder docBuilder) {
        return getDefaultValue().map(value -> literalText(docBuilder, Json.pretty(value))).orElse("");
    }

    public String displayDescription(MarkupDocBuilder markupDocBuilder) {
        return markupDescription(config.getOpenApiMarkupLanguage(), markupDocBuilder, getDescription());
    }

    public String displayType(MarkupDocBuilder markupDocBuilder) {
        return boldText(markupDocBuilder, getIn());
    }

    public String getDescription() {
        return parameter.getDescription();
    }

    public boolean getRequired() {
        return parameter.getRequired() != null && parameter.getRequired();
    }

    public String getPattern() {
        return Optional.ofNullable(parameter.getSchema()).orElse(new Schema()).getPattern();
    }

    public Map<String, Object> getVendorExtensions() {
        return parameter.getExtensions();
    }

    public String getIn() {
        return WordUtils.capitalize(parameter.getIn());
    }

    public Type getType() {
        return type;
    }

    public List<ObjectType> getInlineDefinitions() {
        return inlineDefinitions;
    }

    /**
     * Retrieves the type of a parameter, or otherwise null
     *
     * @param definitionDocumentResolver the definition document resolver
     * @return the type of the parameter, or otherwise null
     */
    private Type getType(Map<String, Model> definitions, DocumentResolver definitionDocumentResolver) {
        Validate.notNull(parameter, "parameter must not be null!");
        Type type = null;
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            Schema model = bodyParameter.getRequestBody().getContent()
                .values().iterator().next().getSchema();

            if (model != null) {
                type = ModelUtils.getType(ModelUtils.convertToModel(model), 
                    definitions, definitionDocumentResolver);
            } else {
                type = new BasicType("string", bodyParameter.getName());
            }

        } else if (parameter.getSchema() != null) {
            List<String> enums = parameter.getSchema().getEnum();

            if (CollectionUtils.isNotEmpty(enums)) {
                type = new EnumType(parameter.getName(), enums);
            } else {
                type = new BasicType(parameter.getSchema().getType(), parameter.getName(), parameter.getSchema().getFormat());
            }
            if (parameter.getSchema().getType().equals("array")) {
                String collectionFormat = getCollectionFormat(parameter);

                type = new ArrayType(parameter.getName(), new PropertyAdapter(((ArraySchema)parameter.getSchema()).getItems()).getType(definitionDocumentResolver), collectionFormat);
            }
        } else if (parameter.get$ref() != null) {
            String refName = parameter.get$ref();

            type = new RefType(definitionDocumentResolver.apply(refName), new ObjectType(refName, null /* FIXME, not used for now */));
        }
        return type;
    }

    /**
     * Retrieves the default value of a parameter
     *
     * @return the default value of the parameter
     */
    public Optional<Object> getDefaultValue() {
        Validate.notNull(parameter, "parameter must not be null!");
        if (parameter.getSchema() != null) {
            return Optional.ofNullable(parameter.getSchema().getDefault());
        }
        return Optional.empty();
    }

    public Optional<Integer> getMinItems() {
        if (parameter.getSchema() != null) {
            Integer minItems = parameter.getSchema().getMinItems();
            return Optional.ofNullable(minItems);
        }
        return Optional.empty();
    }

    public Optional<Integer> getMaxItems() {
        if (parameter.getSchema() != null) {
            Integer maxItems = parameter.getSchema().getMaxItems();
            return Optional.ofNullable(maxItems);
        }
        return Optional.empty();
    }

    protected String getCollectionFormat(Parameter parameter) {
      if (Parameter.StyleEnum.FORM.equals(parameter.getStyle())) {
          // Ref: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md#style-values
          if (Boolean.TRUE.equals(parameter.getExplode())) { // explode is true (default)
              return "multi";
          } else {
              return "csv";
          }
      } else if (Parameter.StyleEnum.SIMPLE.equals(parameter.getStyle())) {
          return "csv";
      } else if (Parameter.StyleEnum.PIPEDELIMITED.equals(parameter.getStyle())) {
          return "pipe";
      } else if (Parameter.StyleEnum.SPACEDELIMITED.equals(parameter.getStyle())) {
          return "space";
      } else {
          return null;
      }
  }
}
