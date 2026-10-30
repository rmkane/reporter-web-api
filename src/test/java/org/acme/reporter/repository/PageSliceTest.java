package org.acme.reporter.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

class PageSliceTest {

  @Test
  void slicesOffsetLimitAndSort() {
    List<Report> reports =
        List.of(
            report("RPT-2", "Bravo", Instant.parse("2026-02-02T00:00:00Z")),
            report("RPT-1", "Alpha", Instant.parse("2026-03-03T00:00:00Z")),
            report("RPT-3", "Charlie", Instant.parse("2026-01-01T00:00:00Z")));

    Page<Report> page =
        PageSlice.slice(reports, PageRequest.of(0, 2, Sort.by("title").ascending()));

    assertThat(page.getContent()).extracting(Report::title).containsExactly("Alpha", "Bravo");
    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.getNumber()).isZero();
  }

  @Test
  void secondPageKeepsTheSortOrder() {
    List<Report> reports =
        List.of(
            report("RPT-2", "Bravo", Instant.parse("2026-02-02T00:00:00Z")),
            report("RPT-1", "Alpha", Instant.parse("2026-03-03T00:00:00Z")),
            report("RPT-3", "Charlie", Instant.parse("2026-01-01T00:00:00Z")));

    Page<Report> page =
        PageSlice.slice(reports, PageRequest.of(1, 2, Sort.by(Sort.Order.desc("updatedOn"))));

    assertThat(page.getContent()).extracting(Report::id).containsExactly("RPT-3");
  }

  @Test
  void idSortOrdersTheNumericSuffix() {
    List<Report> reports =
        List.of(
            report("RPT-10", "Ten", Instant.parse("2026-01-10T00:00:00Z")),
            report("RPT-2", "Two", Instant.parse("2026-01-02T00:00:00Z")),
            report("RPT-1", "One", Instant.parse("2026-01-01T00:00:00Z")));

    Page<Report> ascending =
        PageSlice.slice(reports, PageRequest.of(0, 10, Sort.by("id").ascending()));
    Page<Report> descending =
        PageSlice.slice(reports, PageRequest.of(0, 10, Sort.by("id").descending()));

    assertThat(ascending.getContent())
        .extracting(Report::id)
        .containsExactly("RPT-1", "RPT-2", "RPT-10");
    assertThat(descending.getContent())
        .extracting(Report::id)
        .containsExactly("RPT-10", "RPT-2", "RPT-1");
  }

  private static Report report(String id, String title, Instant updatedOn) {
    return new Report(
        id,
        title,
        "Body",
        ReportStatus.OPEN,
        List.of("demo"),
        "author",
        "assignee",
        Priority.MEDIUM,
        ReportType.TASK,
        updatedOn.minusSeconds(3600),
        updatedOn);
  }
}
