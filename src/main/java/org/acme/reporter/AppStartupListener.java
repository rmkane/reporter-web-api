package org.acme.reporter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.acme.reporter.config.ApiDocsConfig;
import org.acme.reporter.config.SwaggerUiConfig;
import org.acme.reporter.util.Envs;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  private static final String BASE_URL = "http://localhost:";
  private static final String DEFAULT_PORT = "8080";
  private static final String DEFAULT_CONTEXT_PATH = "";

  private static final String HOME_PATH = "/";
  private static final String REPORTS_PATH = "/reports";
  private static final String HEALTH_ID = "health";

  private final SwaggerUiConfig swagger;
  private final ApiDocsConfig apiDocs;
  private final WebEndpointProperties endpoints;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    Environment environment = event.getApplicationContext().getEnvironment();
    String port = resolvePort(environment);
    String contextPath = resolveContextPath(environment);
    String root = BASE_URL + port + contextPath;
    StartupContext context =
        new StartupContext(
            port,
            root + HOME_PATH,
            root + REPORTS_PATH,
            root + swagger.path(),
            root + apiDocs.path(),
            root + resolveHealthPath(endpoints));
    log.info("\n{}", format(context));
  }

  static String resolvePort(Environment environment) {
    return Envs.resolve(environment, DEFAULT_PORT, "local.server.port", "server.port");
  }

  static String resolveContextPath(Environment environment) {
    return Envs.resolve(environment, DEFAULT_CONTEXT_PATH, "server.servlet.context-path");
  }

  static String resolveHealthPath(WebEndpointProperties endpoints) {
    String health = endpoints.getPathMapping().getOrDefault(HEALTH_ID, HEALTH_ID);
    String base = endpoints.getBasePath();
    if (base == null || base.isBlank()) {
      return HOME_PATH + health;
    }
    return base + HOME_PATH + health;
  }

  public static String format(StartupContext context) {
    return """
        Reporter Web API is ready on port %s

        - Application: %s
        - Reports: %s
        - Swagger UI: %s
        - OpenAPI: %s
        - Health: %s
        """
        .formatted(
            context.port(),
            context.root(),
            context.reports(),
            context.swagger(),
            context.openApi(),
            context.health())
        .strip();
  }
}
