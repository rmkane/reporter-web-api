package org.acme.reporter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    Environment environment = event.getApplicationContext().getEnvironment();
    String port = environment.getProperty("local.server.port", "8080");
    String contextPath = environment.getProperty("server.servlet.context-path", "");
    String root = "http://localhost:" + port + contextPath;
    StartupContext context =
        new StartupContext(
            port, root + "/", root + "/reports", root + "/swagger-ui.html", root + "/v3/api-docs");
    log.info(format(context));
  }

  public static String format(StartupContext context) {
    return """
                Reporter Web API is ready on port %s

                - Application: %s
                - Reports: %s
                - Swagger UI: %s
                - OpenAPI: %s
                """
        .formatted(
            context.port(), context.root(), context.reports(), context.swagger(), context.openApi())
        .strip();
  }
}
