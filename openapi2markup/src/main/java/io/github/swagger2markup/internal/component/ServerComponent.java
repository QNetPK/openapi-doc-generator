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

import io.github.swagger2markup.Labels;
import io.github.swagger2markup.OpenApi2MarkupConverter;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.spi.MarkupComponent;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Optional;

import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.copyMarkupDocBuilder;

public class ServerComponent extends MarkupComponent<ServerComponent.Parameters> {


    public ServerComponent(OpenApi2MarkupConverter.Context context) {
        super(context);
    }

    public static ServerComponent.Parameters parameters(List<Server> servers, int titleLevel) {
        return new ServerComponent.Parameters(servers, titleLevel);
    }

    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, Parameters params) {
        List<Server> servers = params.servers;
        if (servers != null && ! servers.isEmpty()) {
            if (servers.size() == 1) {
              markupDocBuilder.sectionTitleLevel(params.titleLevel, labels.getLabel(Labels.SERVER));
            } else {
              markupDocBuilder.sectionTitleLevel(params.titleLevel, labels.getLabel(Labels.SERVERS));
            }
            MarkupDocBuilder paragraphBuilder = copyMarkupDocBuilder(markupDocBuilder);
            for (Server server : servers) {
              if (server.getUrl() != null) {
                  String description = Optional.ofNullable(server.getDescription()).orElse("-");
                  paragraphBuilder.boldText(description)
                          .textLine(COLON + server.getUrl());
              }
            }
            markupDocBuilder.paragraph(paragraphBuilder.toString(), true);
        }
        return markupDocBuilder;
    }

    public static class Parameters {

        private final int titleLevel;
        private final List<Server> servers;

        public Parameters(List<Server> openApi, int titleLevel) {

            this.servers = Validate.notNull(openApi);
            this.titleLevel = titleLevel;
        }
    }


}
