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
package io.github.swagger2markup.internal.document;

import io.github.swagger2markup.Labels;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.internal.component.*;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.spi.MarkupComponent;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.lang3.Validate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;
import static io.github.swagger2markup.spi.OverviewDocumentExtension.Context;
import static io.github.swagger2markup.spi.OverviewDocumentExtension.Position;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class OverviewDocument extends MarkupComponent<OverviewDocument.Parameters> {

    public static final int SECTION_TITLE_LEVEL = 2;
    private static final String OVERVIEW_ANCHOR = "overview";
    private final VersionInfoComponent versionInfoComponent;
    private final ContactInfoComponent contactInfoComponent;
    private final LicenseInfoComponent licenseInfoComponent;
    private final UriSchemeComponent uriSchemeComponent;
    private final ServerComponent serverComponent;
    private final TagsComponent tagsComponent;
    private final ProducesComponent producesComponent;
    private final ConsumesComponent consumesComponent;
    private final ExternalDocsComponent externalDocsComponent;

    public OverviewDocument(OpenApi2MarkupConverter.Context context) {
        super(context);
        versionInfoComponent = new VersionInfoComponent(context);
        contactInfoComponent = new ContactInfoComponent(context);
        licenseInfoComponent = new LicenseInfoComponent(context);
        serverComponent = new ServerComponent(context);
        uriSchemeComponent = new UriSchemeComponent(context);
        tagsComponent = new TagsComponent(context);
        producesComponent = new ProducesComponent(context);
        consumesComponent = new ConsumesComponent(context);
	    externalDocsComponent = new ExternalDocsComponent((context));
    }

  public static OverviewDocument.Parameters parameters(OpenAPI openApi) {
    return new OverviewDocument.Parameters(openApi);
    }

    /**
     * Builds the overview MarkupDocument.
     *
     * @return the overview MarkupDocument
     */
    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, OverviewDocument.Parameters params) {
    OpenAPI openApi = params.openApi;
    Info info = openApi.getInfo();
        buildDocumentTitle(markupDocBuilder, info.getTitle());
        applyOverviewDocumentExtension(new Context(Position.DOCUMENT_BEFORE, markupDocBuilder));
        buildOverviewTitle(markupDocBuilder, labels.getLabel(Labels.OVERVIEW));
        applyOverviewDocumentExtension(new Context(Position.DOCUMENT_BEGIN, markupDocBuilder));
        buildDescriptionParagraph(markupDocBuilder, info.getDescription());
        buildVersionInfoSection(markupDocBuilder, info);
        buildContactInfoSection(markupDocBuilder, info.getContact());
        buildLicenseInfoSection(markupDocBuilder, info);
        buildUriSchemeSection(markupDocBuilder, openApi);
        buildTagsSection(markupDocBuilder, openApi.getTags());
        buildConsumesSection(markupDocBuilder, getConsumes(openApi));
        buildProducesSection(markupDocBuilder, getProduces(openApi));
        buildExternalDocsSection(markupDocBuilder, openApi.getExternalDocs());
        applyOverviewDocumentExtension(new Context(Position.DOCUMENT_END, markupDocBuilder));
        applyOverviewDocumentExtension(new Context(Position.DOCUMENT_AFTER, markupDocBuilder));
        return markupDocBuilder;
    }

    private Set<String> getProduces(OpenAPI openApi) {
      return Stream.concat(
        Optional.ofNullable(openApi.getPaths().values()).orElse(new Paths().values()).stream()
        .flatMap(p -> Optional.ofNullable(p.readOperationsMap().values()).orElse(new ArrayList<>()).stream())
        .flatMap(q -> Optional.ofNullable(q.getResponses()).orElse(new ApiResponses()).entrySet().stream())
        ,
        Optional.ofNullable(openApi.getComponents().getResponses()).orElse(new HashMap<>()).entrySet().stream())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
    }

    private Set<String> getConsumes(OpenAPI openApi) {
      return Stream.concat(
          Optional.ofNullable(openApi.getPaths().values()).orElse(new Paths().values()).stream()
          .flatMap(p -> Optional.ofNullable(p.readOperationsMap().values()).orElse(new ArrayList<>()).stream())
          .flatMap(q -> Optional.ofNullable(q.getRequestBody()).orElse(new RequestBody().content(new Content())).getContent().entrySet().stream())
          ,
          Optional.ofNullable(openApi.getComponents().getRequestBodies()).orElse(new HashMap<>()).entrySet().stream())
          .map(Map.Entry::getKey)
          .collect(Collectors.toSet());
    }

    private void buildDocumentTitle(MarkupDocBuilder markupDocBuilder, String title) {
        markupDocBuilder.documentTitle(title);
    }

    private void buildOverviewTitle(MarkupDocBuilder markupDocBuilder, String title) {
        markupDocBuilder.sectionTitleWithAnchorLevel1(title, OVERVIEW_ANCHOR);
    }

    void buildDescriptionParagraph(MarkupDocBuilder markupDocBuilder, String description) {
        if (isNotBlank(description)) {
            markupDocBuilder.paragraph(markupDescription(config.getOpenApiMarkupLanguage(), markupDocBuilder, description));
        }
    }

    private void buildVersionInfoSection(MarkupDocBuilder markupDocBuilder, Info info) {
        if (info != null) {
            versionInfoComponent.apply(markupDocBuilder, VersionInfoComponent.parameters(info, SECTION_TITLE_LEVEL));
        }
    }

    private void buildContactInfoSection(MarkupDocBuilder markupDocBuilder, Contact contact) {
        if (contact != null) {
            contactInfoComponent.apply(markupDocBuilder, ContactInfoComponent.parameters(contact, SECTION_TITLE_LEVEL));
        }
    }

    private void buildLicenseInfoSection(MarkupDocBuilder markupDocBuilder, Info info) {
        if (info != null) {
            licenseInfoComponent.apply(markupDocBuilder, LicenseInfoComponent.parameters(info, SECTION_TITLE_LEVEL));
        }
    }

    private void buildUriSchemeSection(MarkupDocBuilder markupDocBuilder, OpenAPI openApi) {
      if (context.getConfig().getOpenApiVersion() == 3) {
        serverComponent.apply(markupDocBuilder, ServerComponent.parameters(openApi.getServers(), SECTION_TITLE_LEVEL));
      } else {
        uriSchemeComponent.apply(markupDocBuilder, UriSchemeComponent.parameters(openApi, SECTION_TITLE_LEVEL));
      }
    }

    private void buildTagsSection(MarkupDocBuilder markupDocBuilder, List<Tag> tags) {
        if (isNotEmpty(tags)) {
            tagsComponent.apply(markupDocBuilder, TagsComponent.parameters(tags, SECTION_TITLE_LEVEL));
        }
    }

    private void buildConsumesSection(MarkupDocBuilder markupDocBuilder, Set<String> consumes) {
        if (isNotEmpty(consumes)) {
            consumesComponent.apply(markupDocBuilder, ConsumesComponent.parameters(consumes, SECTION_TITLE_LEVEL));
        }
    }

    private void buildProducesSection(MarkupDocBuilder markupDocBuilder, Set<String> produces) {
        if (isNotEmpty(produces)) {
            producesComponent.apply(markupDocBuilder, ProducesComponent.parameters(produces, SECTION_TITLE_LEVEL));
        }
    }

    private void buildExternalDocsSection(MarkupDocBuilder markupDocBuilder, ExternalDocumentation externalDocs) {
	    if (externalDocs != null) {
	    	externalDocsComponent.apply(markupDocBuilder, ExternalDocsComponent.parameters(externalDocs, SECTION_TITLE_LEVEL));
	    }
    }

    /**
     * Apply extension context to all OverviewContentExtension
     *
     * @param context context
     */
    private void applyOverviewDocumentExtension(Context context) {
        extensionRegistry.getOverviewDocumentExtensions().forEach(extension -> extension.apply(context));
    }

    public static class Parameters {
    private final OpenAPI openApi;

    public Parameters(OpenAPI openApi) {
      this.openApi = Validate.notNull(openApi, "OpenAPI must not be null");
        }
    }

}
