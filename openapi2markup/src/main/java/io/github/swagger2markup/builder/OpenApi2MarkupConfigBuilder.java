/*
 * Copyright 2017 Robert Winkler
 * Modified December 12 2016 by Cas Eliëns
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
package io.github.swagger2markup.builder;

import com.google.common.collect.Ordering;

import io.github.swagger2markup.*;
import io.github.swagger2markup.markup.builder.LineSeparator;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.github.swagger2markup.OpenApi2MarkupProperties.*;

public class OpenApi2MarkupConfigBuilder {

    static final Ordering<PathOperation> OPERATION_METHOD_NATURAL_ORDERING = Ordering
            .explicit(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD, HttpMethod.OPTIONS)
            .onResultOf(PathOperation::getMethod);
    static final Ordering<PathOperation> OPERATION_PATH_NATURAL_ORDERING = Ordering
            .natural()
            .onResultOf(PathOperation::getPath);
    static final Ordering<Parameter> PARAMETER_IN_NATURAL_ORDERING = Ordering
            .explicit("header", "path", "query", "formData", "body")
            .onResultOf(Parameter::getIn);
    static final Ordering<Parameter> PARAMETER_NAME_NATURAL_ORDERING = Ordering
            .natural()
            .onResultOf(Parameter::getName);
    private static final String PROPERTIES_DEFAULT = "io/github/swagger2markup/config/default.properties";
    DefaultOpenApi2MarkupConfig config = new DefaultOpenApi2MarkupConfig();

    public OpenApi2MarkupConfigBuilder() {
        this(new PropertiesConfiguration());
    }

    public OpenApi2MarkupConfigBuilder(Properties properties) {
        this(ConfigurationConverter.getConfiguration(properties));
    }

    public OpenApi2MarkupConfigBuilder(Map<String, String> map) {
        this(new MapConfiguration(map));
    }

    public OpenApi2MarkupConfigBuilder(Configuration configuration) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(new SystemConfiguration());
        compositeConfiguration.addConfiguration(configuration);
        compositeConfiguration.addConfiguration(getDefaultConfiguration());

        OpenApi2MarkupProperties openApi2MarkupProperties = new OpenApi2MarkupProperties(compositeConfiguration);

        config.listDelimiterEnabled = openApi2MarkupProperties.getBoolean(LIST_DELIMITER_ENABLED, false);
        config.listDelimiter = openApi2MarkupProperties.getString(LIST_DELIMITER, ",").charAt(0);
        
        if (config.listDelimiterEnabled && configuration instanceof AbstractConfiguration) {
            ((AbstractConfiguration)configuration).setListDelimiterHandler(new DefaultListDelimiterHandler(config.listDelimiter));
        }

        config.markupLanguage = openApi2MarkupProperties.getRequiredMarkupLanguage(MARKUP_LANGUAGE);
        config.swaggerMarkupLanguage = openApi2MarkupProperties.getRequiredMarkupLanguage(SWAGGER_MARKUP_LANGUAGE);
        config.generatedExamplesEnabled = openApi2MarkupProperties.getRequiredBoolean(GENERATED_EXAMPLES_ENABLED);
        config.hostnameEnabled = openApi2MarkupProperties.getRequiredBoolean(HOSTNAME_ENABLED);
        config.basePathPrefixEnabled = openApi2MarkupProperties.getRequiredBoolean(BASE_PATH_PREFIX_ENABLED);
        config.separatedDefinitionsEnabled = openApi2MarkupProperties.getRequiredBoolean(SEPARATED_DEFINITIONS_ENABLED);
        config.separatedOperationsEnabled = openApi2MarkupProperties.getRequiredBoolean(SEPARATED_OPERATIONS_ENABLED);
        config.pathsGroupedBy = openApi2MarkupProperties.getGroupBy(PATHS_GROUPED_BY);
        config.outputLanguage = openApi2MarkupProperties.getLanguage(OUTPUT_LANGUAGE);
        config.inlineSchemaEnabled = openApi2MarkupProperties.getRequiredBoolean(INLINE_SCHEMA_ENABLED);
        config.interDocumentCrossReferencesEnabled = openApi2MarkupProperties.getRequiredBoolean(INTER_DOCUMENT_CROSS_REFERENCES_ENABLED);
        config.interDocumentCrossReferencesPrefix = openApi2MarkupProperties.getString(INTER_DOCUMENT_CROSS_REFERENCES_PREFIX, null);
        config.flatBodyEnabled = openApi2MarkupProperties.getRequiredBoolean(FLAT_BODY_ENABLED);
        config.pathSecuritySectionEnabled = openApi2MarkupProperties.getRequiredBoolean(PATH_SECURITY_SECTION_ENABLED);
        config.anchorPrefix = openApi2MarkupProperties.getString(ANCHOR_PREFIX, null);
        config.overviewDocument = openApi2MarkupProperties.getRequiredString(OVERVIEW_DOCUMENT);
        config.pathsDocument = openApi2MarkupProperties.getRequiredString(PATHS_DOCUMENT);
        config.definitionsDocument = openApi2MarkupProperties.getRequiredString(DEFINITIONS_DOCUMENT);
        config.securityDocument = openApi2MarkupProperties.getRequiredString(SECURITY_DOCUMENT);
        config.separatedOperationsFolder = openApi2MarkupProperties.getRequiredString(SEPARATED_OPERATIONS_FOLDER);
        config.separatedDefinitionsFolder = openApi2MarkupProperties.getRequiredString(SEPARATED_DEFINITIONS_FOLDER);
        config.tagOrderBy = openApi2MarkupProperties.getOrderBy(TAG_ORDER_BY);
        config.operationOrderBy = openApi2MarkupProperties.getOrderBy(OPERATION_ORDER_BY);
        config.definitionOrderBy = openApi2MarkupProperties.getOrderBy(DEFINITION_ORDER_BY);
        config.parameterOrderBy = openApi2MarkupProperties.getOrderBy(PARAMETER_ORDER_BY);
        config.propertyOrderBy = openApi2MarkupProperties.getOrderBy(PROPERTY_ORDER_BY);
        config.responseOrderBy = openApi2MarkupProperties.getOrderBy(RESPONSE_ORDER_BY);
        Optional<String> lineSeparator = openApi2MarkupProperties.getString(LINE_SEPARATOR);
        if (lineSeparator.isPresent() && StringUtils.isNoneBlank(lineSeparator.get())) {
            config.lineSeparator = LineSeparator.valueOf(lineSeparator.get());
        }

        config.pageBreakLocations = openApi2MarkupProperties.getPageBreakLocations(PAGE_BREAK_LOCATIONS);

        Optional<Pattern> headerPattern = openApi2MarkupProperties.getHeaderPattern(HEADER_REGEX);

        config.headerPattern = headerPattern.orElse(null);
        
        Configuration openApi2markupConfiguration = compositeConfiguration.subset(PROPERTIES_PREFIX);
        Configuration extensionsConfiguration = openApi2markupConfiguration.subset(EXTENSION_PREFIX);
        config.extensionsProperties = new OpenApi2MarkupProperties(extensionsConfiguration);
        config.asciidocPegdownTimeoutMillis = openApi2MarkupProperties.getRequiredInt(ASCIIDOC_PEGDOWN_TIMEOUT);

        config.openApiVersion = openApi2MarkupProperties.getRequiredInt(OPENAPI_VERSION);
        config.producesConsumesEnabled = openApi2MarkupProperties.getRequiredBoolean(PRODUCES_CONSUMES_ENABLED);
        config.tagsSectionEnabled = openApi2MarkupProperties.getRequiredBoolean(TAGS_SECTION_ENABLED);
        config.generatedOptionalQueryParameterExampleEnabled = openApi2MarkupProperties.getRequiredBoolean(GENERATED_OPTIONAL_QUERY_PARAMETER_EXAMPLE_ENABLED);
    }

    /**
     * Loads the default properties from the classpath.
     *
     * @return the default properties
     */
    private Configuration getDefaultConfiguration() {
        Configurations configs = new Configurations();
        try {
            return configs.properties(PROPERTIES_DEFAULT);
        } catch (ConfigurationException e) {
            throw new RuntimeException(String.format("Can't load default properties '%s'", PROPERTIES_DEFAULT), e);
        }
    }

    /**
     * Builds the OpenApi2MarkupConfig.
     *
     * @return the OpenApi2MarkupConfig
     */
    public OpenApi2MarkupConfig build() {
        buildNaturalOrdering();

        return config;
    }

    private void buildNaturalOrdering() {
        if (config.tagOrderBy == OrderBy.NATURAL)
            config.tagOrdering = Ordering.natural();
        if (config.operationOrderBy == OrderBy.NATURAL)
            config.operationOrdering = OPERATION_PATH_NATURAL_ORDERING.compound(OPERATION_METHOD_NATURAL_ORDERING);
        if (config.definitionOrderBy == OrderBy.NATURAL)
            config.definitionOrdering = Ordering.natural();
        if (config.parameterOrderBy == OrderBy.NATURAL)
            config.parameterOrdering = PARAMETER_IN_NATURAL_ORDERING.compound(PARAMETER_NAME_NATURAL_ORDERING);
        if (config.propertyOrderBy == OrderBy.NATURAL)
            config.propertyOrdering = Ordering.natural();
        if (config.responseOrderBy == OrderBy.NATURAL)
            config.responseOrdering = Ordering.natural();
    }

    /**
     * Specifies the markup language which should be used to generate the files.
     *
     * @param markupLanguage the markup language which is used to generate the files
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withMarkupLanguage(MarkupLanguage markupLanguage) {
        Validate.notNull(markupLanguage, "%s must not be null", "markupLanguage");
        config.markupLanguage = markupLanguage;
        return this;
    }

    /**
     * Specifies the markup language used in OpenAPI descriptions.
     *
     * @param swaggerMarkupLanguage the markup language used in OpenAPI descriptions
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withOpenAPIMarkupLanguage(MarkupLanguage swaggerMarkupLanguage) {
        Validate.notNull(swaggerMarkupLanguage, "%s must not be null", "swaggerMarkupLanguage");
        config.swaggerMarkupLanguage = swaggerMarkupLanguage;
        return this;
    }

    /**
     * Include generated examples into the documents.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withGeneratedExamples() {
        config.generatedExamplesEnabled = true;
        return this;
    }

    /**
     * Include generated examples for optional query parameters into the documents.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withGeneratedOptionalQueryParameterExample() {
        config.generatedOptionalQueryParameterExampleEnabled = true;
        return this;
    }
    
    /**
     * In addition to the Definitions file, also create separate definition files for each model definition.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withSeparatedDefinitions() {
        config.separatedDefinitionsEnabled = true;
        return this;
    }


    /**
     * In addition to the Paths file, also create separate operation files for each operation.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withSeparatedOperations() {
        config.separatedOperationsEnabled = true;
        return this;
    }
    
    /**
     * Allows properties to contain a list of elements delimited by a specified character.
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withListDelimiter() {
        config.listDelimiterEnabled = true;
        return this;
    }
    
    /**
     * Specifies the list delimiter which should be used.
     * 
     * @param delimiter the delimiter
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withListDelimiter(Character delimiter) {
        Validate.notNull(delimiter, "%s must not be null", "delimiter");
        config.listDelimiter = delimiter;
        config.listDelimiterEnabled = true;
        return this;
    }


    /**
     * Specifies if the paths should be grouped by tags or stay as-is.
     *
     * @param pathsGroupedBy the GroupBy enum
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withPathsGroupedBy(GroupBy pathsGroupedBy) {
        Validate.notNull(pathsGroupedBy, "%s must not be null", "pathsGroupedBy");
        config.pathsGroupedBy = pathsGroupedBy;
        return this;
    }

    /**
     * Specifies the regex pattern to use for grouping paths.
     *
     * @param headerRegex regex pattern string containing one capture group
     * @return this builder
     * @throws PatternSyntaxException when pattern cannot be compiled
     */
    public OpenApi2MarkupConfigBuilder withHeaderRegex(String headerRegex) {
        Validate.notNull(headerRegex, "%s must not be null", headerRegex);
        config.headerPattern = Pattern.compile(headerRegex);
        return this;
    }

    /**
     * Specifies labels language of output files.
     *
     * @param language the enum
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withOutputLanguage(Language language) {
        Validate.notNull(language, "%s must not be null", "language");
        config.outputLanguage = language;
        return this;
    }

    /**
     * Disable inline schema support.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withoutInlineSchema() {
        config.inlineSchemaEnabled = false;
        return this;
    }

    /**
     * Specifies tag ordering.<br>
     * By default tag ordering == {@link io.github.swagger2markup.OrderBy#NATURAL}.<br>
     * Use {@link #withTagOrdering(Comparator)} to set a custom ordering.
     *
     * @param orderBy tag ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withTagOrdering(OrderBy orderBy) {
        Validate.notNull(orderBy, "%s must not be null", "orderBy");
        Validate.isTrue(orderBy != OrderBy.CUSTOM, "You must provide a custom comparator if orderBy == OrderBy.CUSTOM");
        config.tagOrderBy = orderBy;
        return this;
    }

    /**
     * Specifies a custom comparator function to order tags.
     *
     * @param tagOrdering tag ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withTagOrdering(Comparator<String> tagOrdering) {
        Validate.notNull(tagOrdering, "%s must not be null", "tagOrdering");
        config.tagOrderBy = OrderBy.CUSTOM;
        config.tagOrdering = tagOrdering;
        return this;
    }

    /**
     * Specifies operation ordering.<br>
     * By default operation ordering == {@link io.github.swagger2markup.OrderBy#AS_IS}.<br>
     * Use {@link #withOperationOrdering(Comparator)} to set a custom ordering.
     *
     * @param orderBy operation ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withOperationOrdering(OrderBy orderBy) {
        Validate.notNull(orderBy, "%s must not be null", "orderBy");
        Validate.isTrue(orderBy != OrderBy.CUSTOM, "You must provide a custom comparator if orderBy == OrderBy.CUSTOM");
        config.operationOrderBy = orderBy;
        return this;
    }

    /**
     * Specifies a custom comparator function to order operations.
     *
     * @param operationOrdering operation ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withOperationOrdering(Comparator<PathOperation> operationOrdering) {
        Validate.notNull(operationOrdering, "%s must not be null", "operationOrdering");
        config.operationOrderBy = OrderBy.CUSTOM;
        config.operationOrdering = operationOrdering;
        return this;
    }

    /**
     * Specifies definition ordering.<br>
     * By default definition ordering == {@link io.github.swagger2markup.OrderBy#NATURAL}.<br>
     * Use {@link #withDefinitionOrdering(Comparator)} to set a custom ordering.
     *
     * @param orderBy definition ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withDefinitionOrdering(OrderBy orderBy) {
        Validate.notNull(orderBy, "%s must not be null", "orderBy");
        Validate.isTrue(orderBy != OrderBy.CUSTOM, "You must provide a custom comparator if orderBy == OrderBy.CUSTOM");
        config.definitionOrderBy = orderBy;
        return this;
    }

    /**
     * Specifies a custom comparator function to order definitions.
     *
     * @param definitionOrdering definition ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withDefinitionOrdering(Comparator<String> definitionOrdering) {
        Validate.notNull(definitionOrdering, "%s must not be null", "definitionOrdering");
        config.definitionOrderBy = OrderBy.CUSTOM;
        config.definitionOrdering = definitionOrdering;
        return this;
    }

    /**
     * Specifies parameter ordering.<br>
     * By default parameter ordering == {@link OrderBy#NATURAL}.<br>
     * Use {@link #withParameterOrdering(Comparator)} to set a custom ordering.
     *
     * @param orderBy parameter ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withParameterOrdering(OrderBy orderBy) {
        Validate.notNull(orderBy, "%s must not be null", "orderBy");
        Validate.isTrue(orderBy != OrderBy.CUSTOM, "You must provide a custom comparator if orderBy == OrderBy.CUSTOM");
        config.parameterOrderBy = orderBy;
        return this;
    }

    /**
     * Specifies a custom comparator function to order parameters.
     *
     * @param parameterOrdering parameter ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withParameterOrdering(Comparator<Parameter> parameterOrdering) {
        Validate.notNull(parameterOrdering, "%s must not be null", "parameterOrdering");

        config.parameterOrderBy = OrderBy.CUSTOM;
        config.parameterOrdering = parameterOrdering;
        return this;
    }

    /**
     * Specifies property ordering.<br>
     * By default property ordering == {@link io.github.swagger2markup.OrderBy#NATURAL}.<br>
     * Use {@link #withPropertyOrdering(Comparator)} to set a custom ordering.
     *
     * @param orderBy property ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withPropertyOrdering(OrderBy orderBy) {
        Validate.notNull(orderBy, "%s must not be null", "orderBy");
        Validate.isTrue(orderBy != OrderBy.CUSTOM, "You must provide a custom comparator if orderBy == OrderBy.CUSTOM");
        config.propertyOrderBy = orderBy;
        return this;
    }

    /**
     * Specifies a custom comparator function to order properties.
     *
     * @param propertyOrdering property ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withPropertyOrdering(Comparator<String> propertyOrdering) {
        Validate.notNull(propertyOrdering, "%s must not be null", "propertyOrdering");

        config.propertyOrderBy = OrderBy.CUSTOM;
        config.propertyOrdering = propertyOrdering;
        return this;
    }

    /**
     * Specifies response ordering.<br>
     * By default response ordering == {@link io.github.swagger2markup.OrderBy#NATURAL}.<br>
     * Use {@link #withResponseOrdering(Comparator)} to set a custom ordering.
     *
     * @param orderBy response ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withResponseOrdering(OrderBy orderBy) {
        Validate.notNull(orderBy, "%s must not be null", "orderBy");
        Validate.isTrue(orderBy != OrderBy.CUSTOM, "You must provide a custom comparator if orderBy == OrderBy.CUSTOM");
        config.responseOrderBy = orderBy;
        return this;
    }

    /**
     * Specifies a custom comparator function to order responses.
     *
     * @param responseOrdering response ordering
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withResponseOrdering(Comparator<String> responseOrdering) {
        Validate.notNull(responseOrdering, "%s must not be null", "responseOrdering");

        config.responseOrderBy = OrderBy.CUSTOM;
        config.responseOrdering = responseOrdering;
        return this;
    }

    /**
     * Enable use of inter-document cross-references when needed.
     *
     * @param prefix Prefix to document in all inter-document cross-references.
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withInterDocumentCrossReferences(String prefix) {
        Validate.notNull(prefix, "%s must not be null", "prefix");
        config.interDocumentCrossReferencesEnabled = true;
        config.interDocumentCrossReferencesPrefix = prefix;
        return this;
    }

    /**
     * Enable use of inter-document cross-references when needed.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withInterDocumentCrossReferences() {
        config.interDocumentCrossReferencesEnabled = true;
        return this;
    }

    /**
     * Optionally isolate the body parameter, if any, from other parameters.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withFlatBody() {
        config.flatBodyEnabled = true;
        return this;
    }

    /**
     * Optionally disable the security section for path sections
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withoutPathSecuritySection() {
        config.pathSecuritySectionEnabled = false;
        return this;
    }

    /**
     * Prepend the base path to all paths.
     *
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withBasePathPrefix() {
        config.basePathPrefixEnabled = true;
        return this;
    }

    /**
     * Optionally prefix all anchors for uniqueness.
     *
     * @param anchorPrefix anchor prefix.
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withAnchorPrefix(String anchorPrefix) {
        Validate.notNull(anchorPrefix, "%s must not be null", "anchorPrefix");
        config.anchorPrefix = anchorPrefix;
        return this;
    }

    /**
     * Set the page break locations
     *
     * @param locations List of locations to create new pages
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withPageBreaks(List<PageBreakLocations> locations) {
        Validate.notNull(locations, "%s must not be null", "locations");
        config.pageBreakLocations = locations;
        return this;
    }

    /**
     * Specifies the line separator which should be used.
     *
     * @param lineSeparator the lineSeparator
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withLineSeparator(LineSeparator lineSeparator) {
        Validate.notNull(lineSeparator, "%s must no be null", "lineSeparator");
        config.lineSeparator = lineSeparator;
        return this;
    }

    /**
     * Specifies the output OpenAPI compatibility version.
     * 
     * @param version - the integer version
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withOpenApiVersion(int version) {
      Validate.exclusiveBetween(1, 4, version);
      config.openApiVersion = version;
      return this;
    }

    /**
     * Specifies whether to suppress produce/consume sections.
     * 
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withoutProducesConsumes() {
      config.producesConsumesEnabled = false;
      return this;
    }

    /**
     * Specifies whether to suppress Tag sections.
     * 
     * It still allows ordering of operations by tags
     * 
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withoutTagsSection() {
      config.tagsSectionEnabled = false;
      return this;
    }

    /**
     * Override label keys.
     * 
     * @param overridenLabels
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withLabelsOverride(Map<String, String> overriddenLabels) {
      config.labelsOverride = overriddenLabels;
      return this;
    }

    /**
     * With fixed operation order.
     * 
     * @param comma-separated list of operation IDs
     * @return this builder
     */
    public OpenApi2MarkupConfigBuilder withOperationOrder(String operationOrder) {
      List<String> apiOperationList = new ArrayList<>();
      apiOperationList.addAll(Arrays.asList(operationOrder.split(",")));
  
      config.operationOrdering =
          Comparator.comparing(o -> Integer.valueOf(apiOperationList.indexOf(o.getOperation().getOperationId())));
  
      return this;
    }

    static class DefaultOpenApi2MarkupConfig implements OpenApi2MarkupConfig {

        private MarkupLanguage markupLanguage;
        private MarkupLanguage swaggerMarkupLanguage;
        private boolean generatedExamplesEnabled;
        private boolean hostnameEnabled;
        private boolean basePathPrefixEnabled;
        private boolean separatedDefinitionsEnabled;
        private boolean separatedOperationsEnabled;
        private GroupBy pathsGroupedBy;
        private Language outputLanguage;
        private boolean inlineSchemaEnabled;
        private OrderBy tagOrderBy;
        private Comparator<String> tagOrdering;
        private OrderBy operationOrderBy;
        private Comparator<PathOperation> operationOrdering;
        private OrderBy definitionOrderBy;
        private Comparator<String> definitionOrdering;
        private OrderBy parameterOrderBy;
        private Comparator<Parameter> parameterOrdering;
        private OrderBy propertyOrderBy;
        private Comparator<String> propertyOrdering;
        private OrderBy responseOrderBy;
        private Comparator<String> responseOrdering;
        private boolean interDocumentCrossReferencesEnabled;
        private String interDocumentCrossReferencesPrefix;
        private boolean flatBodyEnabled;
        private boolean pathSecuritySectionEnabled;
        private String anchorPrefix;
        private LineSeparator lineSeparator;

        private String overviewDocument;
        private String pathsDocument;
        private String definitionsDocument;
        private String securityDocument;
        private String separatedOperationsFolder;
        private String separatedDefinitionsFolder;
        private Character listDelimiter;
        private boolean listDelimiterEnabled;
        private int asciidocPegdownTimeoutMillis;

        private List<PageBreakLocations> pageBreakLocations;

        private Pattern headerPattern;

        private OpenApi2MarkupProperties extensionsProperties;

        private int openApiVersion;
        private boolean producesConsumesEnabled;
        private boolean tagsSectionEnabled;
        private boolean generatedOptionalQueryParameterExampleEnabled;
        public Map<String, String> labelsOverride;

        @Override
        public MarkupLanguage getMarkupLanguage() {
            return markupLanguage;
        }

        @Override
        public MarkupLanguage getOpenApiMarkupLanguage() {
            return swaggerMarkupLanguage;
        }

        @Override
        public boolean isGeneratedExamplesEnabled() {
            return generatedExamplesEnabled;
        }

        @Override
        public boolean isGeneratedOptionalQueryParameterExampleEnabled() {
            return generatedOptionalQueryParameterExampleEnabled;
        }

        @Override
        public boolean isSeparatedDefinitionsEnabled() {
            return separatedDefinitionsEnabled;
        }

        @Override
        public boolean isSeparatedOperationsEnabled() {
            return separatedOperationsEnabled;
        }

        @Override
        public GroupBy getPathsGroupedBy() {
            return pathsGroupedBy;
        }

        @Override
        public Language getOutputLanguage() {
            return outputLanguage;
        }

        @Override
        public boolean isInlineSchemaEnabled() {
            return inlineSchemaEnabled;
        }

        @Override
        public OrderBy getTagOrderBy() {
            return tagOrderBy;
        }

        @Override
        public Pattern getHeaderPattern() {
            return headerPattern;
        }

        @Override
        public Comparator<String> getTagOrdering() {
            return tagOrdering;
        }

        @Override
        public OrderBy getOperationOrderBy() {
            return operationOrderBy;
        }

        @Override
        public Comparator<PathOperation> getOperationOrdering() {
            return operationOrdering;
        }

        @Override
        public OrderBy getDefinitionOrderBy() {
            return definitionOrderBy;
        }

        @Override
        public Comparator<String> getDefinitionOrdering() {
            return definitionOrdering;
        }

        @Override
        public OrderBy getParameterOrderBy() {
            return parameterOrderBy;
        }

        @Override
        public Comparator<Parameter> getParameterOrdering() {
            return parameterOrdering;
        }

        @Override
        public OrderBy getPropertyOrderBy() {
            return propertyOrderBy;
        }

        @Override
        public Comparator<String> getPropertyOrdering() {
            return propertyOrdering;
        }

        @Override
        public OrderBy getResponseOrderBy() {
            return responseOrderBy;
        }

        @Override
        public Comparator<String> getResponseOrdering() {
            return responseOrdering;
        }

        @Override
        public boolean isInterDocumentCrossReferencesEnabled() {
            return interDocumentCrossReferencesEnabled;
        }

        @Override
        public String getInterDocumentCrossReferencesPrefix() {
            return interDocumentCrossReferencesPrefix;
        }

        @Override
        public boolean isFlatBodyEnabled() {
            return flatBodyEnabled;
        }

        @Override
        public boolean isPathSecuritySectionEnabled() {
            return pathSecuritySectionEnabled;
        }

        @Override
        public String getAnchorPrefix() {
            return anchorPrefix;
        }

        @Override
        public String getOverviewDocument() {
            return overviewDocument;
        }

        @Override
        public String getPathsDocument() {
            return pathsDocument;
        }

        @Override
        public String getDefinitionsDocument() {
            return definitionsDocument;
        }

        @Override
        public String getSecurityDocument() {
            return securityDocument;
        }

        @Override
        public String getSeparatedOperationsFolder() {
            return separatedOperationsFolder;
        }

        @Override
        public String getSeparatedDefinitionsFolder() {
            return separatedDefinitionsFolder;
        }

        @Override
        public LineSeparator getLineSeparator() {
            return lineSeparator;
        }

        @Override
        public Character getListDelimiter() {
            return listDelimiter;
        }
        
        @Override
        public boolean isListDelimiterEnabled() {
            return listDelimiterEnabled;
        }

        @Override
        public OpenApi2MarkupProperties getExtensionsProperties() {
            return extensionsProperties;
        }

        @Override
        public boolean isHostnameEnabled() {
            return hostnameEnabled;
        }

        @Override
        public boolean isBasePathPrefixEnabled() {
            return basePathPrefixEnabled;
        }

        @Override
        public List<PageBreakLocations> getPageBreakLocations() {
            return pageBreakLocations;
        }

        @Override
        public int getAsciidocPegdownTimeoutMillis() {
            return asciidocPegdownTimeoutMillis;
        }

        @Override
        public int getOpenApiVersion() {
          return openApiVersion;
        }

        @Override
        public boolean isProducesConsumesEnabled() {
          return producesConsumesEnabled;
        }

        @Override
        public boolean isTagsSectionEnabled() {
          return tagsSectionEnabled;
        }

        @Override
        public Map<String, String> getLabelsOverride() {
          return labelsOverride;
        }
    }
}