package org.acme.reporter.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.model.Report;

@SpringBootTest
class OpenApiConfigTest {

  @Autowired private OpenAPI openAPI;

  @Autowired private JsonMapper jsonMapper;

  @Test
  void openApiBeanDescribesTheApplication() {
    assertThat(openAPI.getInfo().getTitle()).isEqualTo("Reporter Web API");
    assertThat(openAPI.getInfo().getVersion()).isEqualTo("0.1.0");
    assertThat(openAPI.getServers()).singleElement().extracting(Server::getUrl).isEqualTo("/");
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
