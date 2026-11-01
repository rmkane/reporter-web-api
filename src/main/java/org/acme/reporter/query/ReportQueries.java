package org.acme.reporter.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.JsonNode;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

@UtilityClass
public class ReportQueries {

  private final Set<String> RANGE_OPERATORS = Set.of(">", ">=", "<", "<=");

  private final Set<String> TAG_OPERATORS = Set.of("=", "!=", "~", "!~", "IN");

  public void validate(ReportQuery query) {
    if (query == null) {
      throw new InvalidReportQueryException("Query is required");
    }
    switch (query) {
      case ReportQuery.And and -> {
        requireChildren(and.children(), "AND");
        and.children().forEach(ReportQueries::validate);
      }
      case ReportQuery.Or or -> {
        requireChildren(or.children(), "OR");
        or.children().forEach(ReportQueries::validate);
      }
      case ReportQuery.Not not -> {
        if (not.child() == null) {
          throw new InvalidReportQueryException("NOT requires a query");
        }
        validate(not.child());
      }
      case ReportQuery.Clause clause -> validateClause(clause);
    }
  }

  public boolean matches(Report report, ReportQuery query) {
    return switch (query) {
      case ReportQuery.And and -> and.children().stream().allMatch(child -> matches(report, child));
      case ReportQuery.Or or -> or.children().stream().anyMatch(child -> matches(report, child));
      case ReportQuery.Not not -> !matches(report, not.child());
      case ReportQuery.Clause clause -> matchesClause(report, clause);
    };
  }

  // Checks values as well as shape, so a bad query fails the same way whatever data is stored.
  private void validateClause(ReportQuery.Clause clause) {
    String field = clause.field();
    String operator = clause.operator();
    ReportFields.requireField(field);
    ReportFields.requireOperator(operator);
    JsonNode value = clause.value();
    if (value == null || value.isNull()) {
      throw new InvalidReportQueryException("Clause value is required");
    }
    if ("IN".equals(operator) && !value.isArray()) {
      throw new InvalidReportQueryException("IN requires a list of values");
    }
    if (RANGE_OPERATORS.contains(operator) && value.isArray()) {
      throw new InvalidReportQueryException(operator + " requires a single value");
    }
    if ("tags".equals(field) && !TAG_OPERATORS.contains(operator)) {
      throw new InvalidReportQueryException("Operator " + operator + " is not supported for tags");
    }
    for (String item : values(value)) {
      if (isEnum(field)) {
        rank(field, item);
      } else if (isTimestamp(field)) {
        parseTime(item);
      }
    }
  }

  private boolean matchesClause(Report report, ReportQuery.Clause clause) {
    String operator = clause.operator();
    if ("tags".equals(clause.field())) {
      return matchesTags(report.tags(), operator, clause.value());
    }
    String actual = text(report, clause.field());
    List<String> expected = values(clause.value());
    // A missing value is neither above nor below anything.
    if (RANGE_OPERATORS.contains(operator) && actual == null) {
      return false;
    }
    return switch (operator) {
      case "=" -> expected.stream().anyMatch(value -> equalsValue(clause.field(), actual, value));
      case "!=" -> expected.stream().noneMatch(value -> equalsValue(clause.field(), actual, value));
      case "~" -> actual != null && expected.stream().anyMatch(value -> contains(actual, value));
      case "!~" -> actual == null || expected.stream().noneMatch(value -> contains(actual, value));
      case ">" -> compare(clause.field(), actual, expected.getFirst()) > 0;
      case ">=" -> compare(clause.field(), actual, expected.getFirst()) >= 0;
      case "<" -> compare(clause.field(), actual, expected.getFirst()) < 0;
      case "<=" -> compare(clause.field(), actual, expected.getFirst()) <= 0;
      case "IN" -> expected.stream().anyMatch(value -> equalsValue(clause.field(), actual, value));
      default -> throw new InvalidReportQueryException("Unknown operator: " + operator);
    };
  }

  private boolean matchesTags(List<String> tags, String operator, JsonNode value) {
    List<String> actual = tags == null ? List.of() : tags;
    List<String> expected = values(value);
    return switch (operator) {
      case "=", "IN" ->
          actual.stream().anyMatch(tag -> expected.stream().anyMatch(tag::equalsIgnoreCase));
      case "!=" ->
          actual.stream().noneMatch(tag -> expected.stream().anyMatch(tag::equalsIgnoreCase));
      case "~" ->
          actual.stream().anyMatch(tag -> expected.stream().anyMatch(item -> contains(tag, item)));
      case "!~" ->
          actual.stream().noneMatch(tag -> expected.stream().anyMatch(item -> contains(tag, item)));
      default ->
          throw new InvalidReportQueryException(
              "Operator " + operator + " is not supported for tags");
    };
  }

  private String text(Report report, String field) {
    return switch (field) {
      case "id" -> report.id();
      case "title" -> report.title();
      case "description" -> report.description();
      case "status" -> report.status() == null ? null : report.status().name();
      case "author" -> report.author();
      case "assignee" -> report.assignee();
      case "priority" -> report.priority() == null ? null : report.priority().name();
      case "type" -> report.type() == null ? null : report.type().name();
      case "createdOn" -> report.createdOn() == null ? null : report.createdOn().toString();
      case "updatedOn" -> report.updatedOn() == null ? null : report.updatedOn().toString();
      default -> throw new InvalidReportQueryException("Unknown field: " + field);
    };
  }

  private boolean equalsValue(String field, String actual, String expected) {
    if (actual == null || expected == null) {
      return false;
    }
    if (isTimestamp(field)) {
      // A bare date matches any time on that UTC day.
      LocalDate day = parseDate(expected);
      if (day != null) {
        return LocalDate.ofInstant(Instant.parse(actual), ZoneOffset.UTC).equals(day);
      }
      return parseTime(actual).equals(parseTime(expected));
    }
    return actual.equalsIgnoreCase(expected);
  }

  private boolean contains(String actual, String expected) {
    return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
  }

  private int compare(String field, String actual, String expected) {
    if (isTimestamp(field)) {
      return parseTime(actual).compareTo(parseTime(expected));
    }
    if (isEnum(field)) {
      return Integer.compare(rank(field, actual), rank(field, expected));
    }
    return actual.compareToIgnoreCase(expected);
  }

  private boolean isTimestamp(String field) {
    return "createdOn".equals(field) || "updatedOn".equals(field);
  }

  private boolean isEnum(String field) {
    return "status".equals(field) || "priority".equals(field) || "type".equals(field);
  }

  /** Declared order, so {@code priority > LOW} means MEDIUM, HIGH, and CRITICAL. */
  private int rank(String field, String value) {
    String name = value.toUpperCase(Locale.ROOT);
    try {
      return switch (field) {
        case "status" -> ReportStatus.valueOf(name).ordinal();
        case "priority" -> Priority.valueOf(name).ordinal();
        case "type" -> ReportType.valueOf(name).ordinal();
        default -> throw new InvalidReportQueryException("Unknown field: " + field);
      };
    } catch (IllegalArgumentException ex) {
      throw new InvalidReportQueryException("Unknown " + field + ": " + value);
    }
  }

  private Instant parseTime(String value) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      LocalDate day = parseDate(value);
      if (day == null) {
        throw new InvalidReportQueryException("Invalid date: " + value);
      }
      return day.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
  }

  private LocalDate parseDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private List<String> values(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    if (node.isArray()) {
      List<String> items = new ArrayList<>();
      node.forEach(child -> items.add(scalar(child)));
      return items;
    }
    return List.of(scalar(node));
  }

  private String scalar(JsonNode node) {
    if (node.isString()) {
      return node.stringValue();
    }
    return node.toString().replace("\"", "");
  }

  private void requireChildren(List<ReportQuery> children, String label) {
    if (children == null || children.isEmpty()) {
      throw new InvalidReportQueryException(label + " requires at least one clause");
    }
  }
}
