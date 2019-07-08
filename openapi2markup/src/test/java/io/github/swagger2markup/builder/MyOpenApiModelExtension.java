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
package io.github.swagger2markup.builder;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import io.github.swagger2markup.spi.OpenApiModelExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;

// tag::MyOpenAPIModelExtension[]
public class MyOpenApiModelExtension extends OpenApiModelExtension {

    public void apply(OpenAPI openApi) {
        List<Server> servers = Optional.ofNullable(openApi.getServers()).orElse(new ArrayList<>());
        servers.add(new Server().url("http://newHostName/newBasePath"));
        openApi.setServers(servers);

        Paths paths = openApi.getPaths(); //<2>
        paths.remove("/remove");
        openApi.setPaths(paths);
    }
}
// end::MyOpenAPIModelExtension[]