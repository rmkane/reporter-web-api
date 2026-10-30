package org.acme.reporter.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tools.jackson.databind.JsonNode;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.model.Report;

public final class ReportQueries {

  private ReportQueries() {}

  public static void validate(ReportQuery query) {
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
      case ReportQuery.Clause clause -> {
        ReportFields.requireField(clause.field());
        ReportFields.requireOperator(clause.operator());
        if (clause.value() == null || clause.value().isNull()) {
          throw new InvalidReportQueryException("Clause value is required");
        }
        if ("IN".equals(clause.operator()) && !clause.value().isArray()) {
          throw new InvalidReportQueryException("IN requires a list of values");
        }
      }
    }
  }

  public static boolean matches(Report report, ReportQuery query) {
    return switch (query) {
      case ReportQuery.And and -> and.children().stream().allMatch(child -> matches(report, child));
      case ReportQuery.Or or -> or.children().stream().anyMatch(child -> matches(report, child));
      case ReportQuery.Not not -> !matches(report, not.child());
      case ReportQuery.Clause clause -> matchesClause(report, clause);
    };
  }

  private static boolean matchesClause(Report report, ReportQuery.Clause clause) {
    String operator = clause.operator();
    if ("tags".equals(clause.field())) {
      return matchesTags(report.tags(), operator, clause.value());
    }
    String actual = text(report, clause.field());
    List<String> expected = values(clause.value());
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

  private static boolean matchesTags(List<String> tags, String operator, JsonNode value) {
    List<String> actual = tags == null ? List.of() : tags;
    List<String> expected = values(value);
    return switch (operator) {
      case "=", "IN" ->
          actual.stream()
              .anyMatch(tag -> expected.stream().anyMatch(item -> tag.equalsIgnoreCase(item)));
      case "!=" ->
          actual.stream()
              .noneMatch(tag -> expected.stream().anyMatch(item -> tag.equalsIgnoreCase(item)));
      case "~" ->
          actual.stream().anyMatch(tag -> expected.stream().anyMatch(item -> contains(tag, item)));
      case "!~" ->
          actual.stream().noneMatch(tag -> expected.stream().anyMatch(item -> contains(tag, item)));
      default ->
          throw new InvalidReportQueryException(
              "Operator " + operator + " is not supported for tags");
    };
  }

  private static String text(Report report, String field) {
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

  private static boolean equalsValue(String field, String actual, String expected) {
    if (actual == null || expected == null) {
      return false;
    }
    if (isTimestamp(field)) {
      return parseTime(actual).equals(parseTime(expected));
    }
    return actual.equalsIgnoreCase(expected);
  }

  private static boolean contains(String actual, String expected) {
    return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
  }

  private static int compare(String field, String actual, String expected) {
    if (actual == null) {
      return -1;
    }
    if (isTimestamp(field)) {
      return parseTime(actual).compareTo(parseTime(expected));
    }
    return actual.compareToIgnoreCase(expected);
  }

  private static boolean isTimestamp(String field) {
    return "createdOn".equals(field) || "updatedOn".equals(field);
  }

  private static Instant parseTime(String value) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      try {
        return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
      } catch (DateTimeParseException ex) {
        throw new InvalidReportQueryException("Invalid date: " + value);
      }
    }
  }

  private static List<String> values(JsonNode node) {
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

  private static String scalar(JsonNode node) {
    if (node.isString()) {
      return node.stringValue();
    }
    return node.toString().replace("\"", "");
  }

  private static void requireChildren(List<ReportQuery> children, String label) {
    if (children == null || children.isEmpty()) {
      throw new InvalidReportQueryException(label + " requires at least one clause");
    }
  }
}
