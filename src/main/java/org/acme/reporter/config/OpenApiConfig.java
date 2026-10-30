package org.acme.reporter.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI reporterOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Reporter Web API")
                .version("0.1.0")
                .description("Search, page, and edit demo reports stored in memory."))
        .servers(sameOriginServers());
  }

  /**
   * Servers Swagger UI uses for Try it out.
   *
   * <p>The URL is the relative path {@code /}. Swagger resolves a relative server against the page
   * that loaded the UI, so the call keeps that page's scheme and host. Opening Swagger through an
   * HTTPS port-forward therefore stays on https and on the forwarded host. An absolute address such
   * as {@code http://localhost:8080} would send those calls to the process behind the tunnel.
   *
   * <p>Operation paths already begin with {@code /api}, so this URL stays {@code /} and Try it out
   * lands on {@code /api/reports}.
   */
  private List<Server> sameOriginServers() {
    return List.of(new Server().url("/").description("Same origin as this page"));
  }
}
