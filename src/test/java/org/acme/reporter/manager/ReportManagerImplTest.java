package org.acme.reporter.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.manager.impl.ReportManagerImpl;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.repository.impl.ReportRepositoryImpl;
import org.acme.reporter.web.UpdateReportRequest;

class ReportManagerImplTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final ReportRepositoryImpl repository =
      new ReportRepositoryImpl(
          List.of(
              report("RPT-1", "Login", "john.doe", ReportStatus.OPEN),
              report("RPT-2", "Login", "jane.roe", ReportStatus.OPEN),
              report("RPT-3", "Export", "john.doe", ReportStatus.CLOSED)));
  private final ReportManagerImpl manager = new ReportManagerImpl(repository);

  @Test
  void andQueryKeepsTitleAndAuthor() {
    ReportQuery query =
        jsonMapper.readValue(
            """
                {"type":"and","children":[
                  {"type":"clause","field":"title","operator":"=","value":"Login"},
                  {"type":"clause","field":"author","operator":"=","value":"john.doe"}
                ]}
                """,
            ReportQuery.class);

    assertThat(manager.search(query, PageRequest.of(0, 10)).getContent())
        .extracting(Report::id)
        .containsExactly("RPT-1");
  }

  @Test
  void unknownFieldIsRejected() {
    ReportQuery query =
        jsonMapper.readValue(
            """
                {"type":"clause","field":"secret","operator":"=","value":"x"}
                """,
            ReportQuery.class);

    assertThatThrownBy(() -> manager.search(query, PageRequest.of(0, 10)))
        .isInstanceOf(InvalidReportQueryException.class)
        .hasMessageContaining("secret");
  }

  @Test
  void unknownSortIsRejected() {
    assertThatThrownBy(() -> manager.list(PageRequest.of(0, 10, Sort.by("password"))))
        .isInstanceOf(InvalidReportQueryException.class)
        .hasMessageContaining("password");
  }

  @Test
  void partialUpdateChangesOnlyProvidedFields() {
    Report current = repository.findById("RPT-1").orElseThrow();
    Report updated =
        manager.update(
            current,
            new UpdateReportRequest(
                "Login form", null, null, null, null, null, Priority.HIGH, null));

    assertThat(updated.title()).isEqualTo("Login form");
    assertThat(updated.author()).isEqualTo("john.doe");
    assertThat(updated.status()).isEqualTo(ReportStatus.OPEN);
    assertThat(updated.priority()).isEqualTo(Priority.HIGH);
    assertThat(updated.createdOn()).isEqualTo(current.createdOn());
    assertThat(updated.updatedOn()).isAfter(current.updatedOn());
  }

  private static Report report(String id, String title, String author, ReportStatus status) {
    Instant created = Instant.parse("2026-01-01T00:00:00Z");
    return new Report(
        id,
        title,
        "Body",
        status,
        List.of("login"),
        author,
        "alex.kim",
        Priority.MEDIUM,
        ReportType.BUG,
        created,
        created.plusSeconds(60));
  }
}
