package org.acme.reporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.node.JsonNodeFactory;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.web.CreateReportRequest;
import org.acme.reporter.web.UpdateReportRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportApiIntegrationTest extends AbstractReportApiTest {

  @Test
  void listsReports() {
    ResponseEntity<ReportPage> response =
        get("/api/reports?size=50", new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().content()).hasSize(30);
    assertThat(response.getBody().page().totalElements()).isEqualTo(30);
    assertThat(response.getBody().content()).extracting(Report::id).contains("RPT-1");
  }

  @Test
  void searchesReports() {
    ReportQuery query =
        new ReportQuery.And(
            List.of(
                new ReportQuery.Clause("title", "=", JsonNodeFactory.instance.stringNode("Login")),
                new ReportQuery.Clause(
                    "author", "=", JsonNodeFactory.instance.stringNode("john.doe"))));

    ResponseEntity<ReportPage> response =
        post("/api/reports/search", query, new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().content()).extracting(Report::id).containsExactly("RPT-1");
    assertThat(response.getBody().page().totalElements()).isEqualTo(1);
  }

  @Test
  void roundTripsAReport() {
    CreateReportRequest create =
        new CreateReportRequest(
            "Nightly digest is late",
            "The Monday mail arrives after noon.",
            null,
            List.of("email", "digest"),
            "jane.roe",
            null,
            Priority.HIGH,
            ReportType.BUG);

    ResponseEntity<Report> created = post("/api/reports", create, Report.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    String id = created.getBody().id();

    try {
      ResponseEntity<Report> fetched = get("/api/reports/" + id, Report.class);
      assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(fetched.getBody()).isNotNull();
      assertThat(fetched.getBody().title()).isEqualTo("Nightly digest is late");
      assertThat(fetched.getBody().author()).isEqualTo("jane.roe");
      assertThat(fetched.getBody().status()).isEqualTo(ReportStatus.OPEN);
      assertThat(fetched.getBody().priority()).isEqualTo(Priority.HIGH);
      assertThat(fetched.getBody().type()).isEqualTo(ReportType.BUG);
      assertThat(fetched.getBody().tags()).containsExactly("email", "digest");

      UpdateReportRequest update =
          new UpdateReportRequest(
              null, null, ReportStatus.IN_PROGRESS, null, null, "alex.kim", null, null);
      ResponseEntity<Report> updated = patch("/api/reports/" + id, update, Report.class);
      assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(updated.getBody()).isNotNull();
      assertThat(updated.getBody().status()).isEqualTo(ReportStatus.IN_PROGRESS);
      assertThat(updated.getBody().assignee()).isEqualTo("alex.kim");
      assertThat(updated.getBody().title()).isEqualTo("Nightly digest is late");

      assertThat(delete("/api/reports/" + id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
      assertThat(status(HttpMethod.GET, "/api/reports/" + id)).isEqualTo(HttpStatus.NOT_FOUND);
    } finally {
      delete("/api/reports/" + id);
    }
  }

  private record ReportPage(List<Report> content, PageDetails page) {
    private record PageDetails(long size, long number, long totalElements, long totalPages) {}
  }
}
