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

package io.github.swagger2markup.internal.type;

import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;

/**
 * Complex object abstraction
 */
public class ObjectType extends Type {

    private Map<String, Schema> properties;
    private ObjectTypePolymorphism polymorphism;

    public ObjectType(String name, ObjectTypePolymorphism polymorphism, Map<String, Schema> properties) {
        super(name);
        this.polymorphism = polymorphism;
        this.properties = properties;
    }

    public ObjectType(String name, Map<String, Schema> properties) {
        this(name, new ObjectTypePolymorphism(ObjectTypePolymorphism.Nature.NONE, null), properties);
    }

    @Override
    public String displaySchema(MarkupDocBuilder docBuilder) {
        return "object";
    }

    public ObjectTypePolymorphism getPolymorphism() {
        return polymorphism;
    }

    public void setPolymorphism(ObjectTypePolymorphism polymorphism) {
        this.polymorphism = polymorphism;
    }

    public Map<String, Schema> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Schema> properties) {
        this.properties = properties;
    }
}
