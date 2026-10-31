package org.acme.reporter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppStartupListenerTest {

  @Test
  void formatBuildsTheStartupBlock() {
    StartupContext context =
        new StartupContext(
            "8080",
            "http://localhost:8080/",
            "http://localhost:8080/reports",
            "http://localhost:8080/swagger-ui.html",
            "http://localhost:8080/v3/api-docs",
            "http://localhost:8080/actuator/health");

    assertThat(AppStartupListener.format(context))
        .isEqualTo(
            """
                Reporter Web API is ready on port 8080

                - Application: http://localhost:8080/
                - Reports: http://localhost:8080/reports
                - Swagger UI: http://localhost:8080/swagger-ui.html
                - OpenAPI: http://localhost:8080/v3/api-docs
                - Health: http://localhost:8080/actuator/health""");
  }
}
