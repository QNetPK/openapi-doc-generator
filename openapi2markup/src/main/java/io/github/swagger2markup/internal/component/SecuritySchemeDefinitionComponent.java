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
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.spi.MarkupComponent;
import io.github.swagger2markup.spi.SecurityDocumentExtension;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.apache.commons.lang3.Validate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ch.netzwerg.paleo.ColumnIds.StringColumnId;
import static io.github.swagger2markup.Labels.*;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.copyMarkupDocBuilder;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;
import static io.github.swagger2markup.spi.SecurityDocumentExtension.Position;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class SecuritySchemeDefinitionComponent extends MarkupComponent<SecuritySchemeDefinitionComponent.Parameters> {

    private final TableComponent tableComponent;

    public SecuritySchemeDefinitionComponent(OpenApi2MarkupConverter.Context context) {
        super(context);
        this.tableComponent = new TableComponent(context);
    }

    public static SecuritySchemeDefinitionComponent.Parameters parameters(String securitySchemeDefinitionName,
                                                                          SecurityScheme securitySchemeDefinition,
                                                                          int titleLevel) {
        return new SecuritySchemeDefinitionComponent.Parameters(securitySchemeDefinitionName, securitySchemeDefinition, titleLevel);
    }

    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, Parameters params) {
        String securitySchemeDefinitionName = params.securitySchemeDefinitionName;
        SecurityScheme securitySchemeDefinition = params.securitySchemeDefinition;
        applySecurityDocumentExtension(new SecurityDocumentExtension.Context(Position.SECURITY_SCHEME_BEFORE, markupDocBuilder, securitySchemeDefinitionName, securitySchemeDefinition));
        markupDocBuilder.sectionTitleWithAnchorLevel(params.titleLevel, securitySchemeDefinitionName);
        applySecurityDocumentExtension(new SecurityDocumentExtension.Context(Position.SECURITY_SCHEME_BEGIN, markupDocBuilder, securitySchemeDefinitionName, securitySchemeDefinition));
        String description = securitySchemeDefinition.getDescription();
        if (isNotBlank(description)) {
            markupDocBuilder.paragraph(markupDescription(config.getOpenApiMarkupLanguage(), markupDocBuilder, description));
        }
        buildSecurityScheme(markupDocBuilder, securitySchemeDefinition);
        applySecurityDocumentExtension(new SecurityDocumentExtension.Context(Position.SECURITY_SCHEME_END, markupDocBuilder, securitySchemeDefinitionName, securitySchemeDefinition));
        applySecurityDocumentExtension(new SecurityDocumentExtension.Context(Position.SECURITY_SCHEME_AFTER, markupDocBuilder, securitySchemeDefinitionName, securitySchemeDefinition));
        return markupDocBuilder;
    }

    private MarkupDocBuilder buildSecurityScheme(MarkupDocBuilder markupDocBuilder, SecurityScheme securityScheme) {
        Type type = securityScheme.getType();
        MarkupDocBuilder paragraphBuilder = copyMarkupDocBuilder(markupDocBuilder);

        paragraphBuilder.italicText(labels.getLabel(TYPE)).textLine(COLON + type);

        switch (type) {
          case APIKEY:
            paragraphBuilder.italicText(labels.getLabel(NAME)).textLine(COLON + securityScheme.getName());
            paragraphBuilder.italicText(labels.getLabel(IN)).textLine(COLON + securityScheme.getIn());
            return markupDocBuilder.paragraph(paragraphBuilder.toString(), true);

          case OAUTH2:
            OAuthFlows flows = securityScheme.getFlows();
            List<OAuthFlow> present = new ArrayList<>();
            if (flows.getAuthorizationCode() != null) {present.add(flows.getAuthorizationCode());}
            if (flows.getClientCredentials() != null) {present.add(flows.getClientCredentials());}
            if (flows.getImplicit() != null) {present.add(flows.getImplicit());}
            if (flows.getPassword() != null) {present.add(flows.getPassword());}


            for (OAuthFlow flow : present) {
            paragraphBuilder.italicText(labels.getLabel(FLOW)).textLine(COLON + flow);
              if (isNotBlank(flow.getAuthorizationUrl())) {
                  paragraphBuilder.italicText(labels.getLabel(AUTHORIZATION_URL)).textLine(COLON + flow.getAuthorizationUrl());
            }
              if (isNotBlank(flow.getTokenUrl())) {
                  paragraphBuilder.italicText(labels.getLabel(TOKEN_URL)).textLine(COLON + flow.getTokenUrl());
            }

            markupDocBuilder.paragraph(paragraphBuilder.toString(), true);

              if (flow.getScopes() != null && !flow.getScopes().isEmpty()) {
                StringColumn.Builder nameColumnBuilder = StringColumn.builder(StringColumnId.of(labels.getLabel(NAME_COLUMN)))
                        .putMetaData(TableComponent.WIDTH_RATIO, "3")
                        .putMetaData(TableComponent.HEADER_COLUMN, "true");
                StringColumn.Builder descriptionColumnBuilder = StringColumn.builder(StringColumnId.of(labels.getLabel(DESCRIPTION_COLUMN)))
                        .putMetaData(TableComponent.WIDTH_RATIO, "17")
                        .putMetaData(TableComponent.HEADER_COLUMN, "true");

                  for (Map.Entry<String, String> scope : flow.getScopes().entrySet()) {
                    nameColumnBuilder.add(scope.getKey());
                    descriptionColumnBuilder.add(scope.getValue());
                }

                return tableComponent.apply(markupDocBuilder, TableComponent.parameters(nameColumnBuilder.build(),
                        descriptionColumnBuilder.build()));
            }
            }
            return markupDocBuilder;

          default:
            return markupDocBuilder.paragraph(paragraphBuilder.toString(), true);
        }
    }

    /**
     * Apply extension context to all SecurityContentExtension
     *
     * @param context context
     */
    private void applySecurityDocumentExtension(SecurityDocumentExtension.Context context) {
        extensionRegistry.getSecurityDocumentExtensions().forEach(extension -> extension.apply(context));
    }

    public static class Parameters {
        private final String securitySchemeDefinitionName;
        private final SecurityScheme securitySchemeDefinition;
        private final int titleLevel;

        public Parameters(String securitySchemeDefinitionName,
                          SecurityScheme securitySchemeDefinition,
                          int titleLevel) {
            this.securitySchemeDefinitionName = Validate.notBlank(securitySchemeDefinitionName, "SecuritySchemeName must not be empty");
            this.securitySchemeDefinition = Validate.notNull(securitySchemeDefinition, "SecurityScheme must not be null");
            this.titleLevel = titleLevel;
        }
    }
}
