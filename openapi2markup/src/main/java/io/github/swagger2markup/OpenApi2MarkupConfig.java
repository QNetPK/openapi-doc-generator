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

import io.github.swagger2markup.markup.builder.LineSeparator;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * OpenApi2Markup configuration interface.
 */
public interface OpenApi2MarkupConfig {

    /**
     * Specifies the markup language which should be used to generate the files.
     */
    MarkupLanguage getMarkupLanguage();

    /**
     * Specifies the markup language used in OpenAPI descriptions.<br>
     * By default, {@link io.github.swagger2markup.markup.builder.MarkupLanguage#MARKDOWN} is assumed.
     */
    MarkupLanguage getOpenApiMarkupLanguage();

    /**
     * Include generated examples into the documents.
     */
    boolean isGeneratedExamplesEnabled();

    /**
     * Prepend the hostname to all paths.
     */
    boolean isHostnameEnabled();
    /**
     * Prepend the base path to all paths.
     */
    boolean isBasePathPrefixEnabled();

    /**
     * In addition to the Definitions file, also create separate definition files for each model definition.
     */
    boolean isSeparatedDefinitionsEnabled();

    /**
     * In addition to the Paths file, also create separate operation files for each operation.
     */
    boolean isSeparatedOperationsEnabled();

    /**
     * Specifies if the operations should be grouped by tags or stay as-is.
     */
    GroupBy getPathsGroupedBy();

    /**
     * Specifies labels language of output files.
     */
    Language getOutputLanguage();

    /**
     * Specifies if inline schemas are detailed
     */
    boolean isInlineSchemaEnabled();

    /**
     * Specifies tag ordering.
     */
    OrderBy getTagOrderBy();

    /**
     * Specifies the regex pattern used for header matching
     */
    Pattern getHeaderPattern();

    /**
     * Specifies a custom comparator function to order tags.
     */
    Comparator<String> getTagOrdering();

    /**
     * Specifies operation ordering.
     */
    OrderBy getOperationOrderBy();

    /**
     * Specifies a custom comparator function to order operations.
     */
    Comparator<PathOperation> getOperationOrdering();

    /**
     * Specifies definition ordering.
     */
    OrderBy getDefinitionOrderBy();

    /**
     * Specifies a custom comparator function to order definitions.
     */
    Comparator<String> getDefinitionOrdering();

    /**
     * Specifies parameter ordering.
     */
    OrderBy getParameterOrderBy();

    /**
     * Specifies a custom comparator function to order parameters.
     */
    Comparator<Parameter> getParameterOrdering();

    /**
     * Specifies property ordering.
     */
    OrderBy getPropertyOrderBy();

    /**
     * Specifies a custom comparator function to order properties.
     */
    Comparator<String> getPropertyOrdering();

    /**
     * Specifies response ordering.
     */
    OrderBy getResponseOrderBy();

    /**
     * Specifies a custom comparator function to order responses.
     */
    Comparator<String> getResponseOrdering();

    /**
     * Enable use of inter-document cross-references when needed.
     */
    boolean isInterDocumentCrossReferencesEnabled();

    /**
     * Inter-document cross-references optional prefix.
     */
    String getInterDocumentCrossReferencesPrefix();

    /**
     * Optionally isolate the body parameter, if any, from other parameters.
     */
    boolean isFlatBodyEnabled();

    /**
     * Optionally disable the security section for path sections
     */
    boolean isPathSecuritySectionEnabled();

    /**
     * Optionally prefix all anchors for uniqueness.
     */
    String getAnchorPrefix();

    /**
     * Overview document name (without extension).
     *
     * @return the overview document name (without extension)
     */
    String getOverviewDocument();

    /**
     * Paths document name (without extension).
     *
     * @return the paths document name (without extension)
     */
    String getPathsDocument();

    /**
     * Definitions document name (without extension).
     *
     * @return the definitions document name (without extension)
     */
    String getDefinitionsDocument();

    /**
     * Security document name (without extension).
     *
     * @return the security document name (without extension)
     */
    String getSecurityDocument();

    /**
     * Separated operations sub-folder name.
     *
     * @return the operations sub-folder name
     */
    String getSeparatedOperationsFolder();

    /**
     * Separated definitions sub-folder name.
     *
     * @return the definitions sub-folder name
     */
    String getSeparatedDefinitionsFolder();

    /**
     * Specifies the line separator which should be used.
     *
     * @return the line separator
     */
    LineSeparator getLineSeparator();

    /**
     * Specifies the array element delimiter to use for multi-valued properties.
     * @return the element delimiter if any
     */
    Character getListDelimiter();
    
    /**
     * Optionally allow lists in property values. Uses the {{@link #getListDelimiter()} to 
     * delimit list values.
     * @return whether lists are converted to arrays
     */
    boolean isListDelimiterEnabled();

    /**
     * Returns properties for extensions.
     *
     * @return the extension properties
     */
    OpenApi2MarkupProperties getExtensionsProperties();

    /**
     * Returns the list of page break locations
     *
     * @return List of PageBreakLocations
     */
    List<PageBreakLocations> getPageBreakLocations();

    /**
     * Returns custom timeout value.
     *
     * @return custom timeout value
     */
    int getAsciidocPegdownTimeoutMillis();

    /**
     * Returns OpenAPI version 2 compatibility
     * 
     * @return version compatibility int
     */
    int getOpenApiVersion();

    /**
     * Suppress produce and consume sections for Overview and Operations.
     * 
     * For mostly REST APIs, this is assumed.
     * 
     * @return whether produce/consume sections are suppressed
     */
    boolean isProducesConsumesEnabled();

    /**
     * Enable Tags section in globally in operations.
     * 
     * Enabled by default
     *
     * @return true if enabled
     */
    boolean isTagsSectionEnabled();

    /**
     * When creating an example for a query parameter, also create when not required.
     * 
     * @return true if optional query parameter example will be generated
     */
    boolean isGeneratedOptionalQueryParameterExampleEnabled();

    /**
     * Returns a map of overridden label keys;
     * 
     * @return map of keys and labels
     */
    Map<String, String> getLabelsOverride();
}
