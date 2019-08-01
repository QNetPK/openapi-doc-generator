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

import com.google.common.collect.ImmutableMap;
import io.github.swagger2markup.OpenApi2MarkupConverter.Context;
import io.github.swagger2markup.internal.adapter.PropertyAdapter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.*;
import io.github.swagger2markup.model.ArrayModel;
import io.github.swagger2markup.model.ComposedModel;
import io.github.swagger2markup.model.ModelImpl;
import io.github.swagger2markup.model.RefModel;
import io.github.swagger2markup.utils.IOUtils;
import io.github.swagger2markup.model.Model;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.Validate;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class ModelUtils {

    /**
     * Recursively resolve referenced type if {@code type} is of type RefType
     *
     * @param type type to resolve
     * @return referenced type
     */
    public static Type resolveRefType(Type type) {
        if (type == null)
            return null;

        if (type instanceof RefType)
            return resolveRefType(((RefType) type).getRefType());
        else
            return type;
    }

    /**
     * Retrieves the type of a model, or otherwise null
     *
     * @param model                      the model
     * @param definitionDocumentResolver the definition document resolver
     * @return the type of the model, or otherwise null
     */
    public static Type getType(Model model, Map<String, Model> definitions, DocumentResolver definitionDocumentResolver) {
        Validate.notNull(model, "model must not be null!");
        if (model instanceof ModelImpl) {
          ModelImpl modelImpl = (ModelImpl) model;

            if (modelImpl.getAdditionalProperties() != null && modelImpl.getAdditionalProperties() instanceof Schema)
                return new MapType(modelImpl.getTitle(), new PropertyAdapter((Schema) modelImpl.getAdditionalProperties()).getType(definitionDocumentResolver));
            else if (modelImpl.getEnum() != null)
                return new EnumType(modelImpl.getTitle(), modelImpl.getEnum());
            else if (modelImpl.getProperties() != null) {
                ObjectType objectType = new ObjectType(modelImpl.getTitle(), model.getProperties());

                String toSet = null;
                if (modelImpl.getDiscriminator() != null) {
                  toSet = modelImpl.getDiscriminator().getPropertyName();
                }
                objectType.getPolymorphism().setDiscriminator(toSet);

                return objectType;
            } else if (modelImpl.getType() == null)
                return null;
            else if (isNotBlank(modelImpl.getFormat()))
                return new BasicType(modelImpl.getType(), modelImpl.getTitle(), modelImpl.getFormat());
            else
                return new BasicType(modelImpl.getType(), modelImpl.getTitle());
        } else if (model instanceof ComposedModel) {
            ComposedModel composedModel = (ComposedModel) model;
            Map<String, Schema> allProperties = new LinkedHashMap<>();
            ObjectTypePolymorphism polymorphism = new ObjectTypePolymorphism(ObjectTypePolymorphism.Nature.NONE, null);
            String name = model.getTitle();

            if (composedModel.getAllOf() != null) {
                polymorphism.setNature(ObjectTypePolymorphism.Nature.COMPOSITION);

                for (Model innerModel : ModelUtils.convertToModelList(composedModel.getAllOf())) {
                    Type innerModelType = resolveRefType(getType(innerModel, definitions, definitionDocumentResolver));

                    if (innerModelType != null) {
                        name = innerModelType.getName();
                    }

                    if (innerModelType instanceof ObjectType) {

                        String innerModelDiscriminator = ((ObjectType) innerModelType).getPolymorphism().getDiscriminator();
                        if (innerModelDiscriminator != null) {
                            polymorphism.setNature(ObjectTypePolymorphism.Nature.INHERITANCE);
                            polymorphism.setDiscriminator(innerModelDiscriminator);
                        }

                        Map<String, Schema> innerModelProperties = ((ObjectType) innerModelType).getProperties();
                        if (innerModelProperties != null)
                            allProperties.putAll(ImmutableMap.copyOf(innerModelProperties));
                    }
                }
            }

            return new ObjectType(name, polymorphism, allProperties);
        } else if (model.get$ref() != null) {
            String refName = model.get$ref();

            Type refType = new ObjectType(refName, null);
            Model referredTo = null;
            String name = null;
            if (definitions.containsKey(refName)) {
                referredTo = definitions.get(refName);
                refType = getType(referredTo, definitions, definitionDocumentResolver);
                name = referredTo.getTitle();
                if (name == null) {
                  name = IOUtils.getNameFromDefinitionPath(refName);
                }
            }
            refType.setName(name);
            refType.setUniqueName(refName);
            RefType retRefType = new RefType(definitionDocumentResolver.apply(refName), refType);
            retRefType.setName(name);
            retRefType.setUniqueName(refName);
            return retRefType;
        } else if (model instanceof ArrayModel) {
            ArrayModel arrayModel = ((ArrayModel) model);

            return new ArrayType(null, new PropertyAdapter(arrayModel.getItems()).getType(definitionDocumentResolver));
        }

        return null;
    }

    public static List<Model> convertToModelList(List<Schema> allOf) {
      return Optional.ofNullable(allOf).orElse(new ArrayList<>())
          .stream()
          .map(a -> convertToModel(a))
          .collect(Collectors.toList());
    }

    public static Map<String, Model> convertToModelMap(Map<String, Schema> schemas) {
      return schemas.entrySet().stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> convertToModel(e.getValue())));
    }

    public static <T> Model<T> convertToModel(Schema<T> schema) {
      if (schema == null) {
        return null;
      }

      try {
        if (schema instanceof ArraySchema) {
          ArraySchema as = (ArraySchema) schema;
          ArrayModel am = new ArrayModel(as.getType(), as.getFormat());
          BeanUtils.copyProperties(am, as);
          if (am.getName() == null) {
            am.setName(as.getTitle());
          }
          return (Model<T>) am;
        }

        if (schema instanceof ComposedSchema) {
          ComposedSchema coms = (ComposedSchema) schema;
          ComposedModel cm = new ComposedModel();
          BeanUtils.copyProperties(cm, coms);
          if (cm.getName() == null) {
            cm.setName(coms.getTitle());
          }
          return (Model<T>) cm;
        }

        if (schema.get$ref() != null) {
          RefModel rm = new RefModel();
          BeanUtils.copyProperties(rm, schema);
          if (rm.getName() == null) {
            rm.setName(schema.getTitle());
          }
          return (Model<T>) rm;
        }

        if (("object".equals(schema.getType()) || schema.getType() == null) && schema.getProperties() != null) {
          ModelImpl<Object> om = new ModelImpl<>("object", null);
          BeanUtils.copyProperties(om, schema);
          om.setType("object");
          if (om.getName() == null) {
            om.setName(schema.getTitle());
          }
          return (Model<T>) om;
        }

        Model<T> cs = new ModelImpl<>(schema.getType(), schema.getFormat());
        BeanUtils.copyProperties(cs, schema);
        return cs;

      } catch (IllegalAccessException | InvocationTargetException e1) {
        throw new IllegalArgumentException("BeanUtil conversion", e1);
      } 
    }

    /**
     * Convert the pre-defined schemas into ref keys and models.
     * 
     * @param context
     * @return a map of references and models
     */
    public static Map<String, Model> getComponentModels(Context context) {
      return convertToModelMap(
          Optional.ofNullable(context.getOpenApi().getComponents().getSchemas())
                .orElse(new HashMap<>())
            .entrySet().stream()
            .collect(Collectors.toMap(e -> "#/components/schemas/" + e.getKey(), e -> e.getValue()))
          );
    }

    public static void distributeRequired(ObjectType modelType, Model model) {
      Validate.notNull(model);
      List<String> requiredFields = model.getRequired();
      if (requiredFields != null) {
        Iterator<Map.Entry<String, Schema>> it = modelType.getProperties().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Schema> entry = it.next();
            if (requiredFields.contains(entry.getKey())) {
              // FIXME - kludge with current OpenAPI implementation
                Schema s = entry.getValue().required(Collections.EMPTY_LIST);
                entry.setValue(s);
            }
        }
      }
    }
}
