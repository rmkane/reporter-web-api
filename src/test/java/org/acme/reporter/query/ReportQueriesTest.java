package org.acme.reporter.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

class ReportQueriesTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final List<Report> reports =
      List.of(
          report("RPT-1", "Login fails", "alex.kim", Priority.LOW, ReportStatus.OPEN, "login"),
          report("RPT-2", "Export slow", "sam.patel", Priority.MEDIUM, ReportStatus.CLOSED, "perf"),
          report("RPT-3", "Login slow", null, Priority.HIGH, ReportStatus.IN_PROGRESS, "LOGIN"),
          report("RPT-4", "Crash", "alex.kim", Priority.CRITICAL, ReportStatus.RESOLVED));

  @ParameterizedTest(name = "{0} {1} {2}")
  @CsvSource(
      delimiter = '|',
      value = {
        "title    | =  | '\"login fails\"'          | RPT-1",
        "title    | != | '\"Login fails\"'          | RPT-2,RPT-3,RPT-4",
        "title    | ~  | '\"SLOW\"'                 | RPT-2,RPT-3",
        "title    | !~ | '\"login\"'                | RPT-2,RPT-4",
        "assignee | IN | '[\"alex.kim\",\"nobody\"]' | RPT-1,RPT-4",
        "id       | >  | '\"RPT-2\"'                | RPT-3,RPT-4",
        "priority | >  | '\"low\"'                  | RPT-2,RPT-3,RPT-4",
        "priority | >= | '\"HIGH\"'                 | RPT-3,RPT-4",
        "priority | <  | '\"HIGH\"'                 | RPT-1,RPT-2",
        "priority | <= | '\"MEDIUM\"'               | RPT-1,RPT-2",
        "status   | <  | '\"RESOLVED\"'             | RPT-1,RPT-3",
        "tags     | =  | '\"login\"'                | RPT-1,RPT-3",
        "tags     | != | '\"login\"'                | RPT-2,RPT-4",
        "tags     | ~  | '\"er\"'                   | RPT-2",
        "tags     | IN | '[\"perf\",\"login\"]'     | RPT-1,RPT-2,RPT-3",
      })
  void operatorsMatch(String field, String operator, String value, String expected) {
    assertThat(ids(clause(field, operator, value))).containsExactly(expected.split(","));
  }

  @Test
  void rangeQueriesSkipMissingValues() {
    assertThat(ids(clause("assignee", "<", "\"z\""))).containsExactly("RPT-1", "RPT-2", "RPT-4");
    assertThat(ids(clause("assignee", ">", "\"a\""))).containsExactly("RPT-1", "RPT-2", "RPT-4");
  }

  @Test
  void negatedMatchesIncludeMissingValues() {
    assertThat(ids(clause("assignee", "!~", "\"alex\""))).containsExactly("RPT-2", "RPT-3");
    assertThat(ids(clause("assignee", "!=", "\"alex.kim\""))).containsExactly("RPT-2", "RPT-3");
  }

  @Test
  void bareDateMatchesTheWholeDay() {
    assertThat(ids(clause("createdOn", "=", "\"2026-01-02\""))).containsExactly("RPT-2");
    assertThat(ids(clause("createdOn", "!=", "\"2026-01-02\"")))
        .containsExactly("RPT-1", "RPT-3", "RPT-4");
  }

  @Test
  void instantMatchesExactly() {
    assertThat(ids(clause("createdOn", "=", "\"2026-01-02T15:30:00Z\""))).containsExactly("RPT-2");
    assertThat(ids(clause("createdOn", "=", "\"2026-01-02T00:00:00Z\""))).isEmpty();
  }

  @Test
  void datesCompareChronologically() {
    assertThat(ids(clause("createdOn", ">=", "\"2026-01-03\""))).containsExactly("RPT-3", "RPT-4");
    assertThat(ids(clause("updatedOn", "<", "\"2026-01-02T16:01:00Z\"")))
        .containsExactly("RPT-1", "RPT-2");
  }

  @Test
  void andOrAndNotCombine() {
    ReportQuery query =
        query(
            """
            {"type":"and","children":[
              {"type":"or","children":[
                {"type":"clause","field":"tags","operator":"=","value":"login"},
                {"type":"clause","field":"priority","operator":"=","value":"CRITICAL"}
              ]},
              {"type":"not","child":
                {"type":"clause","field":"status","operator":"=","value":"OPEN"}}
            ]}
            """);

    assertThat(ids(query)).containsExactly("RPT-3", "RPT-4");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      value = {
        "unknown field     | {\"type\":\"clause\",\"field\":\"secret\",\"operator\":\"=\",\"value\":\"x\"}                | Unknown field: secret",
        "unknown operator  | {\"type\":\"clause\",\"field\":\"title\",\"operator\":\"LIKE\",\"value\":\"x\"}               | Unknown operator: LIKE",
        "missing value     | {\"type\":\"clause\",\"field\":\"title\",\"operator\":\"=\"}                                  | Clause value is required",
        "IN without list   | {\"type\":\"clause\",\"field\":\"title\",\"operator\":\"IN\",\"value\":\"x\"}                 | IN requires a list of values",
        "range with list   | {\"type\":\"clause\",\"field\":\"priority\",\"operator\":\">\",\"value\":[\"LOW\"]}           | > requires a single value",
        "range on tags     | {\"type\":\"clause\",\"field\":\"tags\",\"operator\":\">\",\"value\":\"a\"}                   | Operator > is not supported for tags",
        "unknown priority  | {\"type\":\"clause\",\"field\":\"priority\",\"operator\":\"=\",\"value\":\"URGENT\"}           | Unknown priority: URGENT",
        "unknown in status | {\"type\":\"clause\",\"field\":\"status\",\"operator\":\"IN\",\"value\":[\"OPEN\",\"DONE\"]}  | Unknown status: DONE",
        "invalid date      | {\"type\":\"clause\",\"field\":\"createdOn\",\"operator\":\">\",\"value\":\"yesterday\"}      | Invalid date: yesterday",
        "empty AND         | {\"type\":\"and\",\"children\":[]}                                                            | AND requires at least one clause",
        "empty OR          | {\"type\":\"or\",\"children\":[]}                                                             | OR requires at least one clause",
        "empty NOT         | {\"type\":\"not\"}                                                                            | NOT requires a query",
        "nested error      | {\"type\":\"not\",\"child\":{\"type\":\"clause\",\"field\":\"x\",\"operator\":\"=\",\"value\":1}} | Unknown field: x",
      })
  void invalidQueriesAreRejected(String name, String json, String message) {
    ReportQuery query = query(json);

    assertThatThrownBy(() -> ReportQueries.validate(query))
        .isInstanceOf(InvalidReportQueryException.class)
        .hasMessage(message);
  }

  @Test
  void missingQueryIsRejected() {
    assertThatThrownBy(() -> ReportQueries.validate(null))
        .isInstanceOf(InvalidReportQueryException.class)
        .hasMessage("Query is required");
  }

  @Test
  void validQueryPasses() {
    assertThatCode(
            () -> ReportQueries.validate(clause("status", "IN", "[\"open\",\"IN_PROGRESS\"]")))
        .doesNotThrowAnyException();
  }

  private List<String> ids(ReportQuery query) {
    ReportQueries.validate(query);
    return reports.stream()
        .filter(report -> ReportQueries.matches(report, query))
        .map(Report::id)
        .toList();
  }

  private static ReportQuery clause(String field, String operator, String valueJson) {
    return new ReportQuery.Clause(field, operator, JSON.readTree(valueJson));
  }

  private static ReportQuery query(String json) {
    return JSON.readValue(json, ReportQuery.class);
  }

  private static Report report(
      String id,
      String title,
      String assignee,
      Priority priority,
      ReportStatus status,
      String... tags) {
    int day = Integer.parseInt(id.substring(4));
    Instant created = Instant.parse("2026-01-0" + day + "T15:30:00Z");
    return new Report(
        id,
        title,
        "Body",
        status,
        List.of(tags),
        "john.doe",
        assignee,
        priority,
        ReportType.BUG,
        created,
        created.plusSeconds(1800));
  }
}
