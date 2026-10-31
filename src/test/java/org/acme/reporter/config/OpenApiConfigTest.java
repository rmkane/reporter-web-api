package org.acme.reporter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.model.Report;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

  @Autowired private OpenAPI openAPI;

  @Autowired private JsonMapper jsonMapper;

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiBeanDescribesTheApplication() {
    assertThat(openAPI.getInfo().getTitle()).isEqualTo("Reporter Web API");
    assertThat(openAPI.getInfo().getVersion()).isEqualTo("0.1.0");
    assertThat(openAPI.getServers()).singleElement().extracting(Server::getUrl).isEqualTo("/");
  }

  @Test
  void swaggerExamplesUseTheSeedReport() throws Exception {
    JsonNode spec =
        jsonMapper.readTree(
            mockMvc
                .perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

    JsonNode list = spec.path("paths").path("/api/reports").path("get");
    assertThat(parameter(list, "page").path("example").asString()).isEqualTo("0");
    assertThat(parameter(list, "size").path("example").asString()).isEqualTo("10");
    assertThat(parameter(list, "sort").path("example").asString()).isEqualTo("updatedOn,desc");
    assertThat(parameter(list, "sort").path("schema").has("items")).isFalse();
    assertThat(example(list.path("responses").path("200"), "First page").toString())
        .contains("RPT-1");

    JsonNode search = spec.path("paths").path("/api/reports/search").path("post");
    assertThat(parameter(search, "sort").path("example").asString()).isEqualTo("updatedOn,desc");
    assertThat(parameter(search, "sort").path("schema").has("items")).isFalse();
    assertThat(search.path("parameters").findValuesAsString("name")).doesNotContain("pageable");
    assertThat(example(search.path("requestBody"), "Title and author").toString())
        .contains("john.doe");
    assertThat(example(search.path("requestBody"), "High or critical").path("value").toString())
        .contains("CRITICAL");

    JsonNode create = spec.path("paths").path("/api/reports").path("post");
    assertThat(example(create.path("requestBody"), "Bug").toString())
        .contains("Nightly digest is late");
    assertThat(example(create.path("responses").path("201"), "Created").path("id").asString())
        .isEqualTo("RPT-31");

    JsonNode update = spec.path("paths").path("/api/reports/{id}").path("patch");
    assertThat(example(update.path("requestBody"), "Status and assignee").toString())
        .contains("IN_PROGRESS")
        .doesNotContain("title");
    assertThat(parameter(update, "id").path("example").asString()).isEqualTo("RPT-1");
    assertThat(
            example(update.path("responses").path("404"), "Missing report")
                .path("message")
                .asString())
        .isEqualTo("Report RPT-99 was not found");

    assertThat(
            spec.path("components")
                .path("schemas")
                .path("Report")
                .path("properties")
                .path("title")
                .path("example")
                .asString())
        .isEqualTo("Login");
  }

  private static JsonNode parameter(JsonNode operation, String name) {
    for (JsonNode parameter : operation.path("parameters")) {
      if (name.equals(parameter.path("name").asString())) {
        return parameter;
      }
    }
    throw new AssertionError("Missing parameter " + name);
  }

  private static JsonNode example(JsonNode bodyOrResponse, String name) {
    JsonNode content = bodyOrResponse.path("content");
    JsonNode media = content.path("application/json");
    if (media.isMissingNode()) {
      media = content.path("*/*");
    }
    JsonNode example = media.path("examples").path(name).path("value");
    assertThat(example.isMissingNode()).isFalse();
    return example;
  }

  @Test
  void jsonMapperReadsTheSeedDocument() throws Exception {
    try (InputStream input = new ClassPathResource("data/reports.json").getInputStream()) {
      List<Report> reports = jsonMapper.readValue(input, new TypeReference<List<Report>>() {});
      assertThat(jsonMapper).isInstanceOf(JsonMapper.class);
      assertThat(reports).isNotEmpty();
      assertThat(reports.getFirst().id()).isEqualTo("RPT-1");
    }
  }
}
