package org.acme.reporter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.acme.reporter.config.ReporterProperties;
import org.acme.reporter.config.SwaggerUiConfig;

@SpringBootTest(
    properties = {
      "reporter.github=https://github.com/example/reporter-web-api",
      "springdoc.swagger-ui.path=/custom-swagger"
    })
@AutoConfigureMockMvc
class ReporterWebAppTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ReporterProperties properties;

  @Autowired private SwaggerUiConfig swaggerUi;

  @Test
  void contextLoads() {}

  @Test
  void homeAndReportsRender() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Dashboard")))
        .andExpect(content().string(containsString("Login")))
        .andExpect(content().string(containsString(properties.github())))
        .andExpect(content().string(containsString(swaggerUi.path())))
        .andExpect(
            content().string(not(containsString("https://github.com/rmkane/reporter-web-api"))))
        .andExpect(content().string(not(containsString("/swagger-ui.html"))))
        .andExpect(content().string(not(containsString("Breadcrumb"))));
    mockMvc
        .perform(get("/reports"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("query")))
        .andExpect(content().string(containsString("Breadcrumb")))
        .andExpect(content().string(containsString("aria-current=\"page\"")));
    mockMvc
        .perform(get("/reports/RPT-1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Login")))
        .andExpect(content().string(containsString("Breadcrumb")))
        .andExpect(content().string(containsString("RPT-1")));
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"status\":\"UP\"")));
    mockMvc
        .perform(get("/missing-page").accept(MediaType.TEXT_HTML))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("Not Found")))
        .andExpect(content().string(containsString("Back to the dashboard")));
  }
}
