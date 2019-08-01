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

package io.github.swagger2markup.internal.utils;

import io.github.swagger2markup.internal.adapter.ParameterAdapter;
import io.github.swagger2markup.internal.adapter.PropertyAdapter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.ArrayModel;
import io.github.swagger2markup.model.BodyParameter;
import io.github.swagger2markup.model.ComposedModel;
import io.github.swagger2markup.model.Model;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class ExamplesUtil {

    private static final Integer MAX_RECURSION_TO_DISPLAY = 2;

    /**
     * Generates a Map of response examples
     *
     * @param generateMissingExamples specifies the missing examples should be generated
     * @param operation               the OpenAPI Operation
     * @param definitions             the map of definitions
     * @param markupDocBuilder        the markup builder
     * @return map containing response examples.
     */
    public static Map<String, Object> generateResponseExampleMap(boolean generateMissingExamples, PathOperation operation, Map<String, Model> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder) {
        Map<String, Object> examples = new LinkedHashMap<>();
        Map<String, ApiResponse> responses = operation.getOperation().getResponses();
        if (responses != null)
            for (Map.Entry<String, ApiResponse> responseEntry : responses.entrySet()) {
                ApiResponse response = responseEntry.getValue();
                Content content = Optional.ofNullable(response.getContent()).orElse(new Content());
                Set<Entry<String, MediaType>> mTypes = content.entrySet();
                MediaType mt = null;
                Object example = null;
                if (!mTypes.isEmpty()) {
                  mt = mTypes.iterator().next().getValue();
                  example = mt.getExamples();
                  if (example == null) {
                    example = mt.getExample();
                  }
                if (example == null) {
                    if (mt.getSchema() != null) {
                        Schema schema = mt.getSchema();
                        if (schema != null) {
                            example = schema.getExample();

                            if (example == null && schema.get$ref() != null) {
                                String simpleRef = schema.get$ref();
                                example = generateExampleForRefModel(generateMissingExamples, simpleRef, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                            }
                            if (example == null && schema instanceof ArraySchema && generateMissingExamples) {
                                example = generateExampleForArrayProperty((ArraySchema) schema, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                            }
                            if (example == null && schema instanceof ObjectSchema && generateMissingExamples) {
                                example = exampleMapForProperties(((ObjectSchema) schema).getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                            }
                            if (example == null && generateMissingExamples) {
                                example = PropertyAdapter.generateExample(schema, markupDocBuilder, definitions);
                            }
                        }
                    }
                }
                }
                
                if (example != null)
                    examples.put(responseEntry.getKey(), example);

            }

        return examples;
    }

    /**
     * Generates examples for request
     *
     * @param generateMissingExamples specifies the missing examples should be generated
     * @param pathOperation           the OpenAPI Operation
     * @param definitions             the map of definitions
     * @param markupDocBuilder        the markup builder
     * @param generateOptionalQueryParameterExample    generate optional query parameter example
     * @return an Optional with the example content
     */
    public static Map<String, Object> generateRequestExampleMap(boolean generateMissingExamples, PathOperation pathOperation, 
            Map<String, Model> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, boolean generateOptionalQueryParameterExample) {
        Operation operation = pathOperation.getOperation();
        List<Parameter> parameters = Optional.ofNullable(operation.getParameters()).orElse(new ArrayList<>());
        Map<String, Object> examples = new LinkedHashMap<>();

        // Path example should always be included (if generateMissingExamples):
        if (generateMissingExamples) {
            examples.put("path", pathOperation.getPath());
        }
        for (Parameter parameter : parameters) {
            Object example = null;
            if (parameter instanceof BodyParameter) {
                example = getExamplesFromBodyParameter(parameter);
                if (example == null) {
                    Schema schema = parameter.getSchema();
                    if (schema.get$ref() != null) {
                        String simpleRef = schema.get$ref();
                        example = generateExampleForRefModel(generateMissingExamples, simpleRef, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                    } else if (generateMissingExamples) {
                        if (schema instanceof ComposedModel) {
                            //FIXME: getProperties() may throw NullPointerException
                            example = exampleMapForProperties(((ObjectType) ModelUtils.getType(
                                ModelUtils.convertToModel(schema), definitions, definitionDocumentResolver)).getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                        } else if (schema instanceof ArrayModel) {
                            example = generateExampleForArrayModel((ArrayModel) schema, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                        } else {
                            example = schema.getExample();
                            if (example == null) {
                                example = exampleMapForProperties(schema.getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                            }
                        }
                    }
                }
            } else if (parameter.getSchema() != null) {
                if (generateMissingExamples) {
                    Object abstractSerializableParameterExample;
                    abstractSerializableParameterExample = parameter.getExample();
                    if (abstractSerializableParameterExample == null && parameter.getExtensions() != null) {
                        abstractSerializableParameterExample = parameter.getExtensions().get("x-example");
                    }
                    if (abstractSerializableParameterExample == null) {
                        Schema item = null;
                        if (parameter.getSchema() instanceof ArraySchema) {
                          item = ((ArraySchema)parameter.getSchema()).getItems();
                          if (item != null) {
                            abstractSerializableParameterExample = item.getExample();
                            if (abstractSerializableParameterExample == null) {
                                abstractSerializableParameterExample = PropertyAdapter.generateExample(item, markupDocBuilder, definitions);
                            }
                          }
                        }
                        if (abstractSerializableParameterExample == null) {
                            abstractSerializableParameterExample = ParameterAdapter.generateExample(parameter);
                        }
                    }
                    if (parameter instanceof HeaderParameter){
                        example = parameter.getName() +":\"" +((HeaderParameter) parameter).getSchema().getType()+ "\"";
                    } else if (parameter instanceof PathParameter) {
                        String pathExample = (String) examples.get("path");
                        pathExample = pathExample.replace('{' + parameter.getName() + '}', encodeExampleForUrl(abstractSerializableParameterExample));
                        example = pathExample;
                    } else if (parameter instanceof QueryParameter) {
                        if (parameter.getRequired() || generateOptionalQueryParameterExample)
                        {
                            String path = (String) examples.get("path");
                            String separator = path.contains("?") ? "&" : "?";
                            String pathExample = path + separator + parameter.getName() + "=" + encodeExampleForUrl(abstractSerializableParameterExample);
                            examples.put("path", pathExample);
                        }
                    } else {
                        example = abstractSerializableParameterExample;
                    }
                }
            } else if (parameter.get$ref() != null) {
                String simpleRef = parameter.get$ref();
                example = generateExampleForRefModel(generateMissingExamples, simpleRef, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
            }

            if (example != null)
                examples.put(parameter.getIn(), example);
        }

        return examples;
    }

    /**
     * Retrieves example payloads for body parameter either from examples or from vendor extensions.
     * @param parameter parameter to get the examples for 
     * @return examples if found otherwise null
     */
    private static Object getExamplesFromBodyParameter(Parameter parameter) {
        Object examples = ((BodyParameter) parameter).getExamples();
        if (examples == null) {
          examples = parameter.getSchema().getExample();
        }
        if (examples == null && parameter.getExtensions() != null) {
            examples = parameter.getExtensions().get("x-examples");
        }
        if (examples == null && parameter.getSchema().getExtensions() != null) {
          examples = parameter.getSchema().getExtensions().get("x-examples");
       }
        return examples;
    }

    /**
     * Encodes an example value for use in an URL
     *
     * @param example the example value
     * @return encoded example value
     */
    private static String encodeExampleForUrl(Object example) {
        try {
            return URLEncoder.encode(String.valueOf(example), "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Generates an example object from a simple reference
     *
     * @param generateMissingExamples specifies the missing examples should be generated
     * @param simpleRef               the simple reference string
     * @param definitions             the map of definitions
     * @param markupDocBuilder        the markup builder
     * @param refStack                map to detect cyclic references
     * @return returns an Object or Map of examples
     */
    private static Object generateExampleForRefModel(boolean generateMissingExamples, String simpleRef, Map<String, Model> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        Model model = definitions.get(simpleRef);
        Object example = null;
        if (model != null) {
            example = model.getExample();
            if (example == null && generateMissingExamples) {
                if (!refStack.containsKey(simpleRef)) {
                    refStack.put(simpleRef, 1);
                } else {
                    refStack.put(simpleRef, refStack.get(simpleRef) + 1);
                }
                if (refStack.get(simpleRef) <= MAX_RECURSION_TO_DISPLAY) {
                    if (model instanceof ComposedModel) {
                        //FIXME: getProperties() may throw NullPointerException
                        example = exampleMapForProperties(((ObjectType) ModelUtils.getType(model, definitions, definitionDocumentResolver)).getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                    } else {
                        example = exampleMapForProperties(model.getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, refStack);
                    }
                } else {
                    return "...";
                }
                refStack.put(simpleRef, refStack.get(simpleRef) - 1);
            }
        }
        return example;
    }

    /**
     * Generates a map of examples from a map of properties. If defined examples are found, those are used. Otherwise,
     * examples are generated from the type.
     *
     * @param properties       the map of properties
     * @param definitions      the map of definitions
     * @param markupDocBuilder the markup builder
     * @param refStack         map to detect cyclic references
     * @return a Map of examples
     */
    private static Map<String, Object> exampleMapForProperties(Map<String, Schema> properties, Map<String, Model> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        Map<String, Object> exampleMap = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, Schema> property : properties.entrySet()) {
                Object exampleObject = property.getValue().getExample();
                if (exampleObject == null) {
                    if (property.getValue().get$ref() != null) {
                        exampleObject = generateExampleForRefModel(true, property.getValue().get$ref(), definitions, definitionDocumentResolver, markupDocBuilder, refStack);
                    } else if (property.getValue() instanceof ArraySchema) {
                        exampleObject = generateExampleForArrayProperty((ArraySchema) property.getValue(), definitions, definitionDocumentResolver, markupDocBuilder, refStack);
                    } else if (property.getValue() instanceof MapSchema) {
                        exampleObject = generateExampleForMapProperty((MapSchema) property.getValue(), markupDocBuilder, definitions);
                    }
                    if (exampleObject == null) {
                        Schema valueProperty = property.getValue();
                        exampleObject = PropertyAdapter.generateExample(valueProperty, markupDocBuilder, definitions);
                    }
                }
                exampleMap.put(property.getKey(), exampleObject);
            }
        }
        return exampleMap;
    }

    private static Object generateExampleForMapProperty(MapSchema property, MarkupDocBuilder markupDocBuilder, Map<String, Model> definitions) {
        if (property.getExample() != null) {
            return property.getExample();
        }
        Map<String, Object> exampleMap = new LinkedHashMap<>();
        Object valueProperty = property.getAdditionalProperties();
        if (valueProperty instanceof Schema) {
          if (((Schema) valueProperty).getExample() != null) {
            return ((Schema) valueProperty).getExample();
          }
          exampleMap.put("string", PropertyAdapter.generateExample((Schema)valueProperty, markupDocBuilder, definitions));
        }
        return exampleMap;
    }

    private static Object generateExampleForArrayModel(ArrayModel model, Map<String, Model> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        if (model.getExample() != null) {
            return model.getExample();
        } else if (model.getProperties() != null) {
            return new Object[]{exampleMapForProperties(model.getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, refStack)};
        } else {
            Schema itemProperty = model.getItems();
            return getExample(itemProperty, definitions, definitionDocumentResolver, markupDocBuilder, refStack);
        }
    }

    /**
     * Generates examples from an ArraySchema
     *
     * @param value            ArraySchema
     * @param definitions      map of definitions
     * @param markupDocBuilder the markup builder
     * @return array of Object
     */
    private static Object[] generateExampleForArrayProperty(ArraySchema value, Map<String, Model> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        Schema property = value.getItems();
        return getExample(property, definitions, definitionDocumentResolver, markupDocBuilder, refStack);
    }

    /**
     * Get example from a property
     *
     * @param property                   Schema
     * @param definitions                map of definitions
     * @param definitionDocumentResolver DocumentResolver
     * @param markupDocBuilder           the markup builder
     * @param refStack                   reference stack
     * @return array of Object
     */
    private static Object[] getExample(
            Schema property,
            Map<String, Model> definitions,
            DocumentResolver definitionDocumentResolver,
            MarkupDocBuilder markupDocBuilder,
            Map<String, Integer> refStack) {
        if (property.getExample() != null) {
            return new Object[]{property.getExample()};
        } else if (property instanceof ArraySchema) {
            return new Object[]{generateExampleForArrayProperty((ArraySchema) property, definitions, definitionDocumentResolver, markupDocBuilder, refStack)};
        } else if (property.get$ref() != null) {
            return new Object[]{generateExampleForRefModel(true, property.get$ref(), definitions, definitionDocumentResolver, markupDocBuilder, refStack)};
        } else {
            return new Object[]{PropertyAdapter.generateExample(property, markupDocBuilder, definitions)};
        }
    }

    /**
     * Generates examples for string properties or parameters with given format
     *
     * @param format     the format of the string property
     * @param enumValues the enum values
     * @return example
     */
    public static String generateStringExample(String format, List<String> enumValues) {
        if (enumValues == null || enumValues.isEmpty()) {
            if (format == null) {
                return "string";
            } else {
                switch (format) {
                    case "byte":
                        return "Ynl0ZQ==";
                    case "date":
                        return "1970-01-01";
                    case "date-time":
                        return "1970-01-01T00:00:00Z";
                    case "email":
                        return "email@example.com";
                    case "password":
                        return "secret";
                    case "uuid":
                        return "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
                    default:
                        return "string";
                }
            }
        } else {
            return enumValues.get(0);
        }
    }

    /**
     * Generates examples for integer properties - if there are enums, it uses first enum value, returns 0 otherwise.
     *
     * @param enumValues the enum values
     * @return example
     */
    public static Number generateIntegerExample(List<Number> enumValues) {
        if (enumValues == null || enumValues.isEmpty()) {
            return 0;
        } else {
            return enumValues.get(0);
        }
    }

    //TODO: Unused method, make sure this is never used and then remove it.
    //FIXME: getProperties() may throw NullPointerException

}
