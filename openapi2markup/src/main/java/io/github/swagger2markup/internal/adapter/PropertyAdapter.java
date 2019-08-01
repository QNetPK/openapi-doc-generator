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

import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ArrayType;
import io.github.swagger2markup.internal.type.BasicType;
import io.github.swagger2markup.internal.type.EnumType;
import io.github.swagger2markup.internal.type.MapType;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.type.Type;
import io.github.swagger2markup.internal.utils.ExamplesUtil;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.Model;
import io.github.swagger2markup.utils.IOUtils;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class PropertyAdapter {

    private final Schema property;
    private static Logger logger = LoggerFactory.getLogger(PropertyAdapter.class);

    public PropertyAdapter(Schema property) {
        Validate.notNull(property, "property must not be null");
        this.property = property;
    }

    /**
     * Generate a default example value for property.
     *
     * @param property         property
     * @param markupDocBuilder doc builder
     * @param definitions
     * 
     * @return a generated example for the property
     */
    public static Object generateExample(Schema property, MarkupDocBuilder markupDocBuilder,  Map<String, Model> definitions) {

        String exType = property.getType();
        if (property.get$ref() != null) {
          exType = "ref";
        }

        if (exType == null) {
          return "untyped";
        }

        switch (exType) {
            case "integer":
                return ExamplesUtil.generateIntegerExample(property instanceof IntegerSchema ? ((IntegerSchema) property).getEnum() : null);
            case "number":
                return 0.0;
            case "boolean":
                return true;
            case "string":
                return ExamplesUtil.generateStringExample(property.getFormat(), property instanceof StringSchema ? ((StringSchema) property).getEnum() : null);
            case "ref":
                if (property.get$ref() != null) {
                    Validate.notNull(definitions);
                    Schema itemProperty = (Schema) definitions.get(property.get$ref());
                    if (logger.isDebugEnabled()) logger.debug("generateExample RefProperty for " + itemProperty.getName());
                    return markupDocBuilder.copy(false).crossReference(null, property.get$ref(), itemProperty.getTitle()).toString();
                } else {
                    if (logger.isDebugEnabled()) logger.debug("generateExample for ref not RefProperty");
                }
            case "array":
                if (property instanceof ArraySchema) {
                    return generateArrayExample((ArraySchema) property, markupDocBuilder, definitions);
                }
            default:
                return property.getType();
        }
    }

    /**
     * Generate example for an ArraySchema
     *
     * @param property ArraySchema to generate example for
     * @param markupDocBuilder MarkupDocBuilder containing all associated settings
     * @return String example
     */
    private static Object generateArrayExample(ArraySchema property, MarkupDocBuilder markupDocBuilder, Map<String, Model> definitions) {
        Schema itemProperty = property.getItems();
        List<Object> exampleArray = new ArrayList<>();

        exampleArray.add(generateExample(itemProperty, markupDocBuilder, definitions));
        return exampleArray;
    }

    /**
     * Convert a string {@code value} to specified {@code type}.
     *
     * @param value value to convert
     * @param type  target conversion type
     * @return converted value as object
     */
    public static Object convertExample(String value, String type) {
        if (value == null) {
            return null;
        }

        try {
            switch (type) {
                case "integer":
                    return Integer.valueOf(value);
                case "number":
                    return Float.valueOf(value);
                case "boolean":
                    return Boolean.valueOf(value);
                case "string":
                    return value;
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("Value '%s' cannot be converted to '%s'", value, type), e);
        }
    }

    /**
     * Prepares ref-type for type resolution.
     * 
     * @param definitionDocumentResolver
     * @param definitions
     * @return the type of the property
     */
    public Type getType(DocumentResolver definitionDocumentResolver, Map<String, Model> definitions) {
      if (property.get$ref() != null) {
        Model referredTo = definitions.get(property.get$ref());
        RefType retType = new RefType(definitionDocumentResolver.apply(property.get$ref()), new ObjectType(referredTo.getTitle(), referredTo.getProperties()));
        retType.setUniqueName(property.get$ref());
        return retType;
      }
      return getType(definitionDocumentResolver);
    }

    /**
     * Retrieves the type and format of a property.
     *
     * @param definitionDocumentResolver the definition document resolver
     * @return the type of the property
     */
    public Type getType(DocumentResolver definitionDocumentResolver) {
        Type type = null;
        if (property.get$ref() != null) {
            String name = null;
            if (property.getTitle() != null) {
              name = property.getTitle();
            } else if (property.getName() != null) {
              name = property.getName();
            } else {
              name = IOUtils.getNameFromDefinitionPath(property.get$ref());
            }
            ObjectType innerType = new ObjectType(name, null);
            type = new RefType(definitionDocumentResolver.apply(property.get$ref()), innerType);
            type.setUniqueName(property.get$ref());
        } else if (property instanceof ArraySchema) {
            ArraySchema arrayProperty = (ArraySchema) property;
            Schema items = arrayProperty.getItems();
            if (items == null)
                type = new ArrayType(arrayProperty.getTitle(), new ObjectType(null, null)); // FIXME : Workaround for OpenAPI parser issue with composed models (https://github.com/OpenApi2Markup/swagger2markup/issues/150)
            else {
                Type arrayType = new PropertyAdapter(items).getType(definitionDocumentResolver);
                if (arrayType == null)
                    type = new ArrayType(arrayProperty.getTitle(), new ObjectType(null, null)); // FIXME : Workaround for OpenAPI parser issue with composed models (https://github.com/OpenApi2Markup/swagger2markup/issues/150)
                else
                    type = new ArrayType(arrayProperty.getTitle(), new PropertyAdapter(items).getType(definitionDocumentResolver));
            }
        } else if (property instanceof MapSchema) {
          MapSchema mapProperty = (MapSchema) property;
          Object ap = mapProperty.getAdditionalProperties();
          if (ap instanceof Schema) {
            Schema additionalProperties = (Schema) mapProperty.getAdditionalProperties();
            if (additionalProperties == null)
                type = new MapType(mapProperty.getTitle(), new ObjectType(null, null)); // FIXME : Workaround for OpenAPI parser issue with composed models (https://github.com/OpenApi2Markup/swagger2markup/issues/150)
            else
                type = new MapType(mapProperty.getTitle(), new PropertyAdapter(additionalProperties).getType(definitionDocumentResolver));
          }
        } else if (property instanceof StringSchema) {
            StringSchema stringProperty = (StringSchema) property;
            List<String> enums = stringProperty.getEnum();
            if (CollectionUtils.isNotEmpty(enums)) {
                type = new EnumType(stringProperty.getTitle(), enums);
            } else if (isNotBlank(stringProperty.getFormat())) {
                type = new BasicType(stringProperty.getType(), stringProperty.getTitle(), stringProperty.getFormat());
            } else {
                type = new BasicType(stringProperty.getType(), stringProperty.getTitle());
            }
        } else if (property instanceof ObjectSchema) {
            type = new ObjectType(property.getTitle(), ((ObjectSchema) property).getProperties());
        } else if (property instanceof IntegerSchema) {
            IntegerSchema integerProperty = (IntegerSchema) property;
            List<Number> enums = integerProperty.getEnum();
            if (CollectionUtils.isNotEmpty(enums)) {
                // first, convert integer enum values to strings
                List<String> enumValuesAsString = enums.stream().map(String::valueOf).collect(Collectors.toList());
                type = new EnumType(integerProperty.getTitle(), enumValuesAsString);
            } else if (isNotBlank(integerProperty.getFormat())) {
                type = new BasicType(integerProperty.getType(), integerProperty.getTitle(), integerProperty.getFormat());
            } else {
                type = new BasicType(property.getType(), property.getTitle());
            }
        } else {
            if (property.getType() == null) {
                return null;
            } else if (isNotBlank(property.getFormat())) {
                type = new BasicType(property.getType(), property.getTitle(), property.getFormat());
            } else {
                type = new BasicType(property.getType(), property.getTitle());
            }
        }
        return type;
    }

    /**
     * Retrieves the default value of a property
     *
     * @return the default value of the property
     */
    public Optional<Object> getDefaultValue() {
        if (property instanceof BooleanSchema) {
            BooleanSchema booleanSchema = (BooleanSchema) property;
            return Optional.ofNullable(booleanSchema.getDefault());
        } else if (property instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) property;
            return Optional.ofNullable(stringSchema.getDefault());
        } else if (property instanceof NumberSchema) {
            NumberSchema doubleSchema = (NumberSchema) property;
            return Optional.ofNullable(doubleSchema.getDefault());
        } else if (property instanceof IntegerSchema) {
            IntegerSchema integerSchema = (IntegerSchema) property;
            return Optional.ofNullable(integerSchema.getDefault());
        } else if (property instanceof UUIDSchema) {
            UUIDSchema uuidSchema = (UUIDSchema) property;
            return Optional.ofNullable(uuidSchema.getDefault());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the minLength of a property
     *
     * @return the minLength of the property
     */
    public Optional<Integer> getMinlength() {
        if (property instanceof StringSchema) {
            StringSchema stringProperty = (StringSchema) property;
            return Optional.ofNullable(stringProperty.getMinLength());
        } else if (property instanceof UUIDSchema) {
            UUIDSchema uuidProperty = (UUIDSchema) property;
            return Optional.ofNullable(uuidProperty.getMinLength());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the maxLength of a property
     *
     * @return the maxLength of the property
     */
    public Optional<Integer> getMaxlength() {
        if (property instanceof StringSchema) {
            StringSchema stringProperty = (StringSchema) property;
            return Optional.ofNullable(stringProperty.getMaxLength());
        } else if (property instanceof UUIDSchema) {
            UUIDSchema uuidProperty = (UUIDSchema) property;
            return Optional.ofNullable(uuidProperty.getMaxLength());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the pattern of a property
     *
     * @return the pattern of the property
     */
    public Optional<String> getPattern() {
        if (property instanceof StringSchema) {
            StringSchema stringProperty = (StringSchema) property;
            return Optional.ofNullable(stringProperty.getPattern());
        } else if (property instanceof UUIDSchema) {
            UUIDSchema uuidProperty = (UUIDSchema) property;
            return Optional.ofNullable(uuidProperty.getPattern());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the minimum value of a property
     *
     * @return the minimum value of the property
     */
    public Optional<BigDecimal> getMin() {
        if (property instanceof IntegerSchema) {
            IntegerSchema integerProperty = (IntegerSchema) property;
            return Optional.ofNullable(integerProperty.getMinimum() != null ? integerProperty.getMinimum() : null);
        } else if (property instanceof NumberSchema) {
            NumberSchema numericProperty = (NumberSchema) property;
            return Optional.ofNullable(numericProperty.getMinimum());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the exclusiveMinimum value of a property
     *
     * @return the exclusiveMinimum value of the property
     */
    public boolean getExclusiveMin() {
        if (property instanceof NumberSchema) {
            NumberSchema numericProperty = (NumberSchema) property;
            return BooleanUtils.isTrue(numericProperty.getExclusiveMinimum());
        }
        return false;
    }

    /**
     * Retrieves the minimum value of a property
     *
     * @return the minimum value of the property
     */
    public Optional<BigDecimal> getMax() {
        if (property instanceof IntegerSchema) {
            IntegerSchema integerProperty = (IntegerSchema) property;
            return Optional.ofNullable(integerProperty.getMaximum() != null ? integerProperty.getMaximum() : null);
        } else if (property instanceof NumberSchema) {
            NumberSchema numericProperty = (NumberSchema) property;
            return Optional.ofNullable(numericProperty.getMaximum());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the exclusiveMaximum value of a property
     *
     * @return the exclusiveMaximum value of the property
     */
    public boolean getExclusiveMax() {
        if (property instanceof NumberSchema) {
            NumberSchema numericProperty = (NumberSchema) property;
            return BooleanUtils.isTrue((numericProperty.getExclusiveMaximum()));
        }
        return false;
    }

    /**
     * Return example display string for the given {@code property}.
     *
     * @param generateMissingExamples specifies if missing examples should be generated
     * @param markupDocBuilder        doc builder
     * @param definintions 
     * @return property example display string
     */
    public Optional<Object> getExample(boolean generateMissingExamples, MarkupDocBuilder markupDocBuilder, Map<String, Model> definitions) {
        if (property.getExample() != null) {
            return Optional.ofNullable(property.getExample());
        } else if (property instanceof MapSchema) {
          Object ap = ((MapSchema) property).getAdditionalProperties();
          if (ap instanceof Schema) {
            Schema additionalProperty = (Schema) ((MapSchema) property).getAdditionalProperties();
            if (additionalProperty.getExample() != null) {
                return Optional.ofNullable(additionalProperty.getExample());
            } else if (generateMissingExamples) {
                Map<String, Object> exampleMap = new HashMap<>();
                exampleMap.put("string", generateExample(additionalProperty, markupDocBuilder, definitions));
                return Optional.of(exampleMap);
            }
          }
        } else if (property instanceof ArraySchema) {
            if (generateMissingExamples) {
                Schema itemProperty = ((ArraySchema) property).getItems();
                List<Object> exampleArray = new ArrayList<>();
                exampleArray.add(generateExample(itemProperty, markupDocBuilder, definitions));
                return Optional.of(exampleArray);
            }
        } else if (generateMissingExamples) {
            return Optional.of(generateExample(property, markupDocBuilder, definitions));
        }

        return Optional.empty();
    }

    /**
     * Checks if a property is read-only.
     *
     * @return true if the property is read-only
     */
    public boolean getReadOnly() {
        return BooleanUtils.isTrue(property.getReadOnly());
    }

    /**
     * Gets description from property or its ref.
     * 
     * @return description, which may be markup
     */
    public String getDescription(Map<String, Model> definitions) {
      if (StringUtils.isBlank(property.getDescription()) && property.get$ref() != null) {
        return definitions.get(property.get$ref()).getDescription();
      }
      return property.getDescription();
    }
}
