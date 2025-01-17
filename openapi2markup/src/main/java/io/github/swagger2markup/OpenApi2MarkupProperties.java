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
package io.github.swagger2markup;

import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.github.swagger2markup.utils.URIUtils;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.ex.ConversionException;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class OpenApi2MarkupProperties {

    /**
     * Prefix for OpenApi2Markup properties
     */
    public static final String PROPERTIES_PREFIX = "openapi2markup";

    public static final String MARKUP_LANGUAGE = PROPERTIES_PREFIX + ".markupLanguage";
    public static final String SWAGGER_MARKUP_LANGUAGE = PROPERTIES_PREFIX + ".swaggerMarkupLanguage";
    public static final String GENERATED_EXAMPLES_ENABLED = PROPERTIES_PREFIX + ".generatedExamplesEnabled";
    public static final String HOSTNAME_ENABLED = PROPERTIES_PREFIX + ".hostnameEnabled";
    public static final String BASE_PATH_PREFIX_ENABLED = PROPERTIES_PREFIX + ".basePathPrefixEnabled";
    public static final String SEPARATED_DEFINITIONS_ENABLED = PROPERTIES_PREFIX + ".separatedDefinitionsEnabled";
    public static final String SEPARATED_OPERATIONS_ENABLED = PROPERTIES_PREFIX + ".separatedOperationsEnabled";
    public static final String PATHS_GROUPED_BY = PROPERTIES_PREFIX + ".pathsGroupedBy";
    public static final String HEADER_REGEX = PROPERTIES_PREFIX + ".headerRegex";
    public static final String OUTPUT_LANGUAGE = PROPERTIES_PREFIX + ".outputLanguage";
    public static final String INLINE_SCHEMA_ENABLED = PROPERTIES_PREFIX + ".inlineSchemaEnabled";
    public static final String INTER_DOCUMENT_CROSS_REFERENCES_ENABLED = PROPERTIES_PREFIX + ".interDocumentCrossReferencesEnabled";
    public static final String INTER_DOCUMENT_CROSS_REFERENCES_PREFIX = PROPERTIES_PREFIX + ".interDocumentCrossReferencesPrefix";
    public static final String FLAT_BODY_ENABLED = PROPERTIES_PREFIX + ".flatBodyEnabled";
    public static final String PATH_SECURITY_SECTION_ENABLED = PROPERTIES_PREFIX + ".pathSecuritySectionEnabled";
    public static final String ANCHOR_PREFIX = PROPERTIES_PREFIX + ".anchorPrefix";
    public static final String LIST_DELIMITER = PROPERTIES_PREFIX + ".listDelimiter";
    public static final String LIST_DELIMITER_ENABLED = PROPERTIES_PREFIX + ".listDelimiterEnabled";
    public static final String OVERVIEW_DOCUMENT = PROPERTIES_PREFIX + ".overviewDocument";
    public static final String PATHS_DOCUMENT = PROPERTIES_PREFIX + ".pathsDocument";
    public static final String DEFINITIONS_DOCUMENT = PROPERTIES_PREFIX + ".definitionsDocument";
    public static final String SECURITY_DOCUMENT = PROPERTIES_PREFIX + ".securityDocument";
    public static final String SEPARATED_OPERATIONS_FOLDER = PROPERTIES_PREFIX + ".separatedOperationsFolder";
    public static final String SEPARATED_DEFINITIONS_FOLDER = PROPERTIES_PREFIX + ".separatedDefinitionsFolder";
    public static final String TAG_ORDER_BY = PROPERTIES_PREFIX + ".tagOrderBy";
    public static final String OPERATION_ORDER_BY = PROPERTIES_PREFIX + ".operationOrderBy";
    public static final String DEFINITION_ORDER_BY = PROPERTIES_PREFIX + ".definitionOrderBy";
    public static final String PARAMETER_ORDER_BY = PROPERTIES_PREFIX + ".parameterOrderBy";
    public static final String PROPERTY_ORDER_BY = PROPERTIES_PREFIX + ".propertyOrderBy";
    public static final String RESPONSE_ORDER_BY = PROPERTIES_PREFIX + ".responseOrderBy";
    public static final String LINE_SEPARATOR = PROPERTIES_PREFIX + ".lineSeparator";
    public static final String PAGE_BREAK_LOCATIONS = PROPERTIES_PREFIX + ".pageBreakLocations";
    public static final String ASCIIDOC_PEGDOWN_TIMEOUT = PROPERTIES_PREFIX + ".asciidoc.pegdown.timeoutMillis";

    public static final String OPENAPI_VERSION = PROPERTIES_PREFIX + ".openApiVersion";
    public static final String PRODUCES_CONSUMES_ENABLED = PROPERTIES_PREFIX + ".producesConsumesEnabled";
    public static final String TAGS_SECTION_ENABLED = PROPERTIES_PREFIX + ".tagsSectionEnabled";
    public static final String GENERATED_OPTIONAL_QUERY_PARAMETER_EXAMPLE_ENABLED = PROPERTIES_PREFIX + ".generatedOptionalQueryParameterExampleEnabled";

    /**
     * Prefix for OpenApi2Markup extension properties
     */
    public static final String EXTENSION_PREFIX = "extensions";

    private final Configuration configuration;

    public OpenApi2MarkupProperties(Properties properties) {
        this(ConfigurationConverter.getConfiguration(properties));
    }

    public OpenApi2MarkupProperties(Map<String, String> map) {
        this(new MapConfiguration(map));
    }

    public OpenApi2MarkupProperties(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns an optional String property value associated with the given key.
     *
     * @param key the property name to resolve
     * @return The string property
     */
    public Optional<String> getString(String key) {
        return Optional.ofNullable(configuration.getString(key));
    }

    /**
     * Return the String property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key          the property name to resolve
     * @param defaultValue the default value to return if no value is found
     * @return The string property
     */
    public String getString(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

    /**
     * Return the int property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key          the property name to resolve
     * @param defaultValue the default value to return if no value is found
     * @return The int property
     */
    public int getInt(String key, int defaultValue) {
        return configuration.getInt(key, defaultValue);
    }

    /**
     * Returns an optional Integer property value associated with the given key.
     *
     * @param key the property name to resolve
     * @return An optional Integer property
     */
    public Optional<Integer> getInteger(String key) {
        return Optional.ofNullable(configuration.getInteger(key, null));
    }

    /**
     * Return the int property value associated with the given key (never {@code null}).
     *
     * @return The int property
     * @throws IllegalStateException if the key cannot be
     */
    public int getRequiredInt(String key) {
        Optional<Integer> value = getInteger(key);
        if (value.isPresent()) {
            return value.get();
        }
        throw new IllegalStateException(String.format("required key [%s] not found", key));
    }

    /**
     * Return the boolean property value associated with the given key (never {@code null}).
     *
     * @return The boolean property
     * @throws IllegalStateException if the key cannot be resolved
     */
    public boolean getRequiredBoolean(String key) {
        Boolean value = configuration.getBoolean(key, null);
        if (value != null) {
            return value;
        } else {
            throw new IllegalStateException(String.format("required key [%s] not found", key));
        }
    }

    /**
     * Return the boolean property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key          the property name to resolve
     * @param defaultValue the default value to return if no value is found
     * @return The boolean property
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return configuration.getBoolean(key, defaultValue);
    }

    /**
     * Return the URI property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @return The URI property
     * @throws IllegalStateException if the value cannot be mapped to the enum
     */
    public Optional<URI> getURI(String key) {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return Optional.of(URIUtils.create(property.get()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return the URI property value associated with the given key (never {@code null}).
     *
     * @return The URI property
     * @throws IllegalStateException if the key cannot be resolved
     */
    public URI getRequiredURI(String key) {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return URIUtils.create(property.get());
        } else {
            throw new IllegalStateException(String.format("required key [%s] not found", key));
        }
    }

    /**
     * Return the Path property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @return The Path property
     * @throws IllegalStateException if the value cannot be mapped to the enum
     */
    public Optional<Path> getPath(String key) {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return Optional.of(Paths.get(property.get()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return a list of Path property values associated with the given key, 
     * or {@code defaultValue} if the key cannot be resolved.
     * 
     * @param key the property name to resolve
     * @return The list of Path properties
     * @throws IllegalStateException if the value cannot be mapped to an array of strings
     */
    public List<Path> getPathList(String key) {
        List<Path> pathList = new ArrayList<>();

        try {
            String[] stringList = configuration.getStringArray(key);

            for (String pathStr : stringList) {
                pathList.add(Paths.get(pathStr));
            }
        } catch (ConversionException ce) {
            throw new IllegalStateException(String.format("requested key [%s] is not convertable to an array", key));
        }

        return pathList;
    }

    /**
     * Return the Path property value associated with the given key (never {@code null}).
     *
     * @return The Path property
     * @throws IllegalStateException if the key cannot be resolved
     */
    public Path getRequiredPath(String key) {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return Paths.get(property.get());
        } else {
            throw new IllegalStateException(String.format("required key [%s] not found", key));
        }
    }

    /**
     * Return the MarkupLanguage property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @return The MarkupLanguage property
     */
    public Optional<MarkupLanguage> getMarkupLanguage(String key) {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return Optional.of(MarkupLanguage.valueOf(property.get()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return the MarkupLanguage property value associated with the given key (never {@code null}).
     *
     * @return The MarkupLanguage property
     * @throws IllegalStateException if the key cannot be resolved
     */
    public MarkupLanguage getRequiredMarkupLanguage(String key) {
        return MarkupLanguage.valueOf(configuration.getString(key));
    }

    /**
     * Return the Language property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @return The Language property
     */
    public Language getLanguage(String key) {
        return Language.valueOf(configuration.getString(key));
    }

    /**
     * Return the GroupBy property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @return The GroupBy property
     * @throws IllegalStateException if the value cannot be mapped to the enum
     */
    public GroupBy getGroupBy(String key) {
        return GroupBy.valueOf(configuration.getString(key));
    }

    /**
     * Return the OrderBy property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @return The OrderBy property
     * @throws IllegalStateException if the value cannot be mapped to the enum
     */
    public OrderBy getOrderBy(String key) {
        return OrderBy.valueOf(configuration.getString(key));
    }

    /**
     * Return the String property value associated with the given key (never {@code null}).
     *
     * @return The String property
     * @throws IllegalStateException if the key cannot be resolved
     */
    public String getRequiredString(String key) throws IllegalStateException {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return property.get();
        } else {
            throw new IllegalStateException(String.format("required key [%s] not found", key));
        }
    }

    /**
     * Return the list of keys.
     *
     * @return the list of keys.
     */
    public List<String> getKeys() {
        return IteratorUtils.toList(configuration.getKeys());
    }

    /**
     * Get the list of the keys contained in the configuration that match the
     * specified prefix. For instance, if the configuration contains the
     * following keys:<br>
     * {@code swagger2markup.extensions.folder1, swagger2markup.extensions.folder2, swagger2markup.folder3},<br>
     * an invocation of {@code getKeys("swagger2markup.extensions");}<br>
     * will return the key below:<br>
     * {@code swagger2markup.extensions.folder1, swagger2markup.extensions.folder2}.<br>
     * Note that the prefix itself is included in the result set if there is a
     * matching key. The exact behavior - how the prefix is actually
     * interpreted - depends on a concrete implementation.
     *
     * @param prefix The prefix to test against.
     * @return the list of keys.
     */
    public List<String> getKeys(String prefix) {
        return IteratorUtils.toList(configuration.getKeys(prefix));
    }

    public List<PageBreakLocations> getPageBreakLocations(String key) {
        List<PageBreakLocations> result = configuration.getList(PageBreakLocations.class, key);
        if(result == null) result = new ArrayList<PageBreakLocations>();

        return result;
    }

    public Optional<Pattern> getHeaderPattern(String key) {
        Optional<String> property = getString(key);
        if (property.isPresent()) {
            return Optional.of(Pattern.compile(property.get()));
        } else {
            return Optional.empty();
        }
    }
}
