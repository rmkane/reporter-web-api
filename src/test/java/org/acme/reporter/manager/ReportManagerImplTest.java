package org.acme.reporter.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.error.ReportConflictException;
import org.acme.reporter.error.ReportNotFoundException;
import org.acme.reporter.manager.impl.ReportManagerImpl;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;
import org.acme.reporter.model.ReportVersion;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.repository.impl.ReportRepositoryImpl;
import org.acme.reporter.web.CreateReportRequest;
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

  @Test
  void createTrimsTextAndFillsDefaults() {
    Report created =
        manager.create(
            new CreateReportRequest(
                " Crash ", " Body ", null, null, " jane.roe ", "  ", null, null));

    assertThat(created.id()).isEqualTo("RPT-4");
    assertThat(created.title()).isEqualTo("Crash");
    assertThat(created.description()).isEqualTo("Body");
    assertThat(created.author()).isEqualTo("jane.roe");
    assertThat(created.status()).isEqualTo(ReportStatus.OPEN);
    assertThat(created.tags()).isEmpty();
    assertThat(created.assignee()).isNull();
    assertThat(created.createdOn()).isEqualTo(created.updatedOn());
    assertThat(repository.findById("RPT-4")).contains(created);
  }

  @Test
  void nextIdSkipsGapsAndIdsInOtherFormats() {
    ReportManagerImpl manager =
        new ReportManagerImpl(
            new ReportRepositoryImpl(
                List.of(
                    report("RPT-2", "A", "a", ReportStatus.OPEN),
                    report("RPT-7", "B", "b", ReportStatus.OPEN),
                    report("RPT-abc", "C", "c", ReportStatus.OPEN),
                    report("LEGACY-99", "D", "d", ReportStatus.OPEN))));

    assertThat(manager.create(request("New")).id()).isEqualTo("RPT-8");
  }

  @Test
  void nextIdStartsAtOneWhenEmpty() {
    ReportManagerImpl manager = new ReportManagerImpl(new ReportRepositoryImpl(List.of()));

    assertThat(manager.create(request("First")).id()).isEqualTo("RPT-1");
  }

  @Test
  void concurrentCreatesGetDistinctIds() throws Exception {
    int creates = 50;
    try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
      List<Future<Report>> futures = new ArrayList<>();
      for (int i = 0; i < creates; i++) {
        futures.add(pool.submit(() -> manager.create(request("Parallel"))));
      }
      Set<String> ids = new HashSet<>();
      for (Future<Report> future : futures) {
        ids.add(future.get().id());
      }

      assertThat(ids).hasSize(creates);
    }
    assertThat(repository.count()).isEqualTo(3 + creates);
  }

  @Test
  void updateTrimsText() {
    Report current = repository.findById("RPT-1").orElseThrow();
    Report updated =
        manager.update(
            current,
            new UpdateReportRequest(
                "  Login form ", null, null, null, null, " sam.patel ", null, null));

    assertThat(updated.title()).isEqualTo("Login form");
    assertThat(updated.assignee()).isEqualTo("sam.patel");
  }

  @Test
  void checkedUpdateSavesWhenTheVersionMatches() {
    String version = ReportVersion.of(repository.findById("RPT-1").orElseThrow());

    Report updated = manager.update("RPT-1", priority(Priority.LOW), version);

    assertThat(updated.priority()).isEqualTo(Priority.LOW);
    assertThat(ReportVersion.of(updated)).isNotEqualTo(version);
  }

  @Test
  void checkedUpdateRejectsAStaleVersionAndReturnsTheCurrentReport() {
    String loaded = ReportVersion.of(repository.findById("RPT-1").orElseThrow());
    Report changed = manager.update("RPT-1", priority(Priority.LOW), loaded);

    assertThatThrownBy(() -> manager.update("RPT-1", priority(Priority.CRITICAL), loaded))
        .isInstanceOfSatisfying(
            ReportConflictException.class,
            conflict -> assertThat(conflict.getCurrent()).isEqualTo(changed));
    assertThat(repository.findById("RPT-1").orElseThrow().priority()).isEqualTo(Priority.LOW);
  }

  @Test
  void updateWithoutAVersionSkipsTheCheck() {
    assertThat(manager.update("RPT-1", priority(Priority.LOW), null).priority())
        .isEqualTo(Priority.LOW);
  }

  @Test
  void checkedUpdateOfAnUnknownReportIsNotFound() {
    assertThatThrownBy(() -> manager.update("RPT-404", priority(Priority.LOW), null))
        .isInstanceOf(ReportNotFoundException.class);
  }

  @Test
  void everySaveMovesTheVersionForward() {
    Set<String> versions = new HashSet<>();
    for (int i = 0; i < 20; i++) {
      versions.add(ReportVersion.of(manager.update("RPT-1", priority(Priority.LOW), null)));
    }

    assertThat(versions).hasSize(20);
  }

  @Test
  void ifMatchHeadersParseToTheVersion() {
    assertThat(ReportVersion.fromIfMatch("\"2026-01-01T00:00:00Z\""))
        .isEqualTo("2026-01-01T00:00:00Z");
    assertThat(ReportVersion.fromIfMatch("W/\"2026-01-01T00:00:00Z\""))
        .isEqualTo("2026-01-01T00:00:00Z");
    assertThat(ReportVersion.fromIfMatch("*")).isNull();
    assertThat(ReportVersion.fromIfMatch(null)).isNull();
  }

  private static UpdateReportRequest priority(Priority priority) {
    return new UpdateReportRequest(null, null, null, null, null, null, priority, null);
  }

  private static CreateReportRequest request(String title) {
    return new CreateReportRequest(title, "Body", null, null, "jane.roe", null, null, null);
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
