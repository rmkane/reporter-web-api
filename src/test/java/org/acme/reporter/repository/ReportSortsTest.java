package org.acme.reporter.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

class ReportSortsTest {

  @Test
  void unsortedFallsBackToIdOrder() {
    List<Report> reports = List.of(report("RPT-10"), report("RPT-9"), report("RPT-1"));

    assertThat(ids(reports, Sort.unsorted())).containsExactly("RPT-1", "RPT-9", "RPT-10");
  }

  @Test
  void idSortComparesTheNumber() {
    List<Report> reports = List.of(report("RPT-10"), report("RPT-2"), report("RPT-1"));

    assertThat(ids(reports, Sort.by("id").ascending())).containsExactly("RPT-1", "RPT-2", "RPT-10");
    assertThat(ids(reports, Sort.by("id").descending()))
        .containsExactly("RPT-10", "RPT-2", "RPT-1");
  }

  @Test
  void leadingZerosCompareByValue() {
    List<Report> reports = List.of(report("RPT-010"), report("RPT-9"));

    assertThat(ids(reports, Sort.by("id"))).containsExactly("RPT-9", "RPT-010");
  }

  @Test
  void textSortIgnoresCase() {
    List<Report> reports =
        List.of(titled("RPT-1", "bravo"), titled("RPT-2", "Alpha"), titled("RPT-3", "charlie"));

    assertThat(ids(reports, Sort.by("title"))).containsExactly("RPT-2", "RPT-1", "RPT-3");
  }

  @Test
  void timestampsSortChronologically() {
    List<Report> reports =
        List.of(
            updated("RPT-1", "2026-02-02T00:00:00Z"),
            updated("RPT-2", "2026-03-03T00:00:00Z"),
            updated("RPT-3", "2026-01-01T00:00:00Z"));

    assertThat(ids(reports, Sort.by(Sort.Order.desc("updatedOn"))))
        .containsExactly("RPT-2", "RPT-1", "RPT-3");
  }

  @Test
  void prioritySortsBySeverityNotName() {
    List<Report> reports =
        List.of(
            report("RPT-1", Priority.LOW, "a"),
            report("RPT-2", Priority.CRITICAL, "a"),
            report("RPT-3", Priority.MEDIUM, "a"),
            report("RPT-4", Priority.HIGH, "a"));

    assertThat(ids(reports, Sort.by("priority").descending()))
        .containsExactly("RPT-2", "RPT-4", "RPT-3", "RPT-1");
  }

  @Test
  void nullsStayLastInBothDirections() {
    List<Report> reports =
        List.of(
            report("RPT-1", Priority.LOW, null),
            report("RPT-2", Priority.LOW, "bea"),
            report("RPT-3", Priority.LOW, "Ann"));

    assertThat(ids(reports, Sort.by("assignee").ascending()))
        .containsExactly("RPT-3", "RPT-2", "RPT-1");
    assertThat(ids(reports, Sort.by("assignee").descending()))
        .containsExactly("RPT-2", "RPT-3", "RPT-1");
  }

  @Test
  void laterOrdersBreakTies() {
    List<Report> reports =
        List.of(
            report("RPT-1", "Bravo", Priority.HIGH),
            report("RPT-2", "Alpha", Priority.LOW),
            report("RPT-3", "Alpha", Priority.HIGH));

    assertThat(ids(reports, Sort.by(Sort.Order.desc("priority"), Sort.Order.asc("title"))))
        .containsExactly("RPT-3", "RPT-1", "RPT-2");
  }

  @Test
  void unknownPropertyIsRejected() {
    assertThatThrownBy(() -> ReportSorts.of(Sort.by("password")))
        .isInstanceOf(InvalidReportQueryException.class)
        .hasMessage("Unknown sort property: password");
  }

  private static List<String> ids(List<Report> reports, Sort sort) {
    return reports.stream().sorted(ReportSorts.of(sort)).map(Report::id).toList();
  }

  private static Report report(String id) {
    return report(id, "Title", Priority.MEDIUM, "assignee", "2026-01-01T00:00:00Z");
  }

  private static Report titled(String id, String title) {
    return report(id, title, Priority.MEDIUM, "assignee", "2026-01-01T00:00:00Z");
  }

  private static Report updated(String id, String updatedOn) {
    return report(id, "Title", Priority.MEDIUM, "assignee", updatedOn);
  }

  private static Report report(String id, Priority priority, String assignee) {
    return report(id, "Title", priority, assignee, "2026-01-01T00:00:00Z");
  }

  private static Report report(String id, String title, Priority priority) {
    return report(id, title, priority, "assignee", "2026-01-01T00:00:00Z");
  }

  private static Report report(
      String id, String title, Priority priority, String assignee, String updated) {
    Instant updatedOn = Instant.parse(updated);
    return new Report(
        id,
        title,
        "Body",
        ReportStatus.OPEN,
        List.of(),
        "author",
        assignee,
        priority,
        ReportType.TASK,
        updatedOn,
        updatedOn);
  }
}
