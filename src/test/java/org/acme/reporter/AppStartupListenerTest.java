package org.acme.reporter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;

class AppStartupListenerTest {

  @Test
  void formatBuildsTheStartupBlock() {
    StartupContext context =
        new StartupContext(
            "8080",
            "http://localhost:8080/",
            "http://localhost:8080/reports",
            "http://localhost:8080/swagger-ui/index.html",
            "http://localhost:8080/v3/api-docs",
            "http://localhost:8080/actuator/health");

    assertThat(AppStartupListener.format(context))
        .isEqualTo(
            """
                Reporter Web API is ready on port 8080

                - Application: http://localhost:8080/
                - Reports: http://localhost:8080/reports
                - Swagger UI: http://localhost:8080/swagger-ui/index.html
                - OpenAPI: http://localhost:8080/v3/api-docs
                - Health: http://localhost:8080/actuator/health""");
  }

  @Test
  void healthPathFollowsTheActuatorBasePath() {
    WebEndpointProperties endpoints = new WebEndpointProperties();

    assertThat(AppStartupListener.resolveHealthPath(endpoints)).isEqualTo("/actuator/health");

    endpoints.setBasePath("/manage");
    assertThat(AppStartupListener.resolveHealthPath(endpoints)).isEqualTo("/manage/health");

    endpoints.getPathMapping().put("health", "status");
    assertThat(AppStartupListener.resolveHealthPath(endpoints)).isEqualTo("/manage/status");
  }
}
