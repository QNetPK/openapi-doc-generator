package io.github.swagger2markup.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.models.servers.Server;

public class UriScheme {

  private URL url;
  private List<Server> servers;

  public UriScheme(List<Server> servers) {
    this.servers = servers;
    if (servers != null && !servers.isEmpty()) {
      for (Server server : servers) {
        try {
          this.url = new URL(server.getUrl());
          break;
        } catch (MalformedURLException e) {
          continue;
        }
      }
    }
  }

  public CharSequence getHost() {
    return url.getHost();
  }

  public String getBasePath() {
    return url.getPath();
  }

  public Collection<Scheme> getSchemes() {
    return servers.stream().map(s -> {
      try {
        return Scheme.valueOfIgnoreCase(new URL(s.getUrl()).getProtocol());
      } catch (MalformedURLException e) {
        return null;
      }
    }).collect(Collectors.toList());
  }

  public URL getUrl() {
    return url;
  }

  public enum Scheme {
    HTTP, HTTPS;

    public static Scheme valueOfIgnoreCase(String key) {
      if (key != null) {
        return valueOf(key.toUpperCase());
      }
      return null;
    }
  }
}
