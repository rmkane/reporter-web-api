package org.acme.reporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

@Tag("integration")
@TestInstance(Lifecycle.PER_CLASS)
class LocalhostReportIntegrationTest extends AbstractReportApiTest {

  @BeforeAll
  void requireRunningServer() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host(), port()), 1_000);
    } catch (IOException ex) {
      fail(
          "No server is listening at "
              + baseUrl()
              + ". Start the app before the integration tests.",
          ex);
    }
  }

  @Test
  void readsHostPortAndPathFromProperties() {
    System.setProperty("reporter.integration.host", "reports.internal");
    System.setProperty("reporter.integration.port", "9090");
    System.setProperty("reporter.integration.path", "/reporter/");
    try {
      assertThat(baseUrl()).isEqualTo("http://reports.internal:9090");
      assertThat(endpoint("/api/reports?size=50")).isEqualTo("/reporter/api/reports?size=50");
    } finally {
      System.clearProperty("reporter.integration.host");
      System.clearProperty("reporter.integration.port");
      System.clearProperty("reporter.integration.path");
    }
  }

  @Test
  void listsReportsOnLocalhost() throws IOException {
    String body = get("/api/reports?size=50");
    saveJson("reports.json", body);

    JsonNode page = tree(body);
    assertThat(page.path("page").path("totalElements").longValue()).isEqualTo(30);
    assertThat(page.path("content").size()).isEqualTo(30);
    assertThat(ids(page)).contains("RPT-1");
  }

  @Test
  void getsOneReportOnLocalhost() throws IOException {
    String body = get("/api/reports/RPT-1");
    saveJson("report-RPT-1.json", body);

    JsonNode report = tree(body);
    assertThat(report.path("id").stringValue()).isEqualTo("RPT-1");
    assertThat(report.path("title").stringValue()).isEqualTo("Login");
    assertThat(report.path("author").stringValue()).isEqualTo("john.doe");
    assertThat(report.path("status").stringValue()).isEqualTo("OPEN");
  }

  @Test
  void searchesReportsOnLocalhost() throws IOException {
    String query =
        """
        {"type":"and","children":[
          {"type":"clause","field":"title","operator":"=","value":"Login"},
          {"type":"clause","field":"author","operator":"=","value":"john.doe"}
        ]}
        """;
    String body = post("/api/reports/search", query);
    saveJson("reports-search.json", body);

    JsonNode page = tree(body);
    assertThat(page.path("page").path("totalElements").longValue()).isEqualTo(1);
    assertThat(ids(page)).containsExactly("RPT-1");
  }

  @Test
  void insertsUpdatesAndDeletesAReport() throws IOException {
    String created =
        """
        {"title":"Nightly digest is late","description":"The Monday mail arrives after noon.","tags":["email","digest"],"author":"jane.roe","priority":"HIGH","type":"BUG"}
        """;
    Call inserted = post("/api/reports", created, HttpStatus.CREATED.value());
    saveJson("report-created.json", inserted.body());
    JsonNode report = tree(inserted.body());
    String id = report.path("id").stringValue();
    assertThat(id).startsWith("RPT-");
    assertThat(report.path("status").stringValue()).isEqualTo("OPEN");
    assertThat(report.path("title").stringValue()).isEqualTo("Nightly digest is late");

    boolean removed = false;
    try {
      String updated =
          """
          {"status":"IN_PROGRESS","assignee":"alex.kim"}
          """;
      Call patched = patch("/api/reports/" + id, updated, HttpStatus.OK.value());
      saveJson("report-updated.json", patched.body());
      JsonNode afterUpdate = tree(patched.body());
      assertThat(afterUpdate.path("status").stringValue()).isEqualTo("IN_PROGRESS");
      assertThat(afterUpdate.path("assignee").stringValue()).isEqualTo("alex.kim");
      assertThat(afterUpdate.path("title").stringValue()).isEqualTo("Nightly digest is late");

      assertThat(delete("/api/reports/" + id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
      removed = true;
      get("/api/reports/" + id, HttpStatus.NOT_FOUND.value());
    } finally {
      if (!removed) {
        delete("/api/reports/" + id);
      }
    }
  }
}
