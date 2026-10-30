package org.acme.reporter.query;

import java.util.Set;

import org.springframework.data.domain.Sort;

import org.acme.reporter.error.InvalidReportQueryException;

public final class ReportFields {

  public static final Set<String> NAMES =
      Set.of(
          "id",
          "title",
          "description",
          "status",
          "tags",
          "author",
          "assignee",
          "priority",
          "type",
          "createdOn",
          "updatedOn");

  private static final Set<String> OPERATORS =
      Set.of("=", "!=", "~", "!~", ">", ">=", "<", "<=", "IN");

  private ReportFields() {}

  public static void requireSort(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return;
    }
    for (Sort.Order order : sort) {
      if (!NAMES.contains(order.getProperty())) {
        throw new InvalidReportQueryException("Unknown sort property: " + order.getProperty());
      }
    }
  }

  public static void requireOperator(String operator) {
    if (operator == null || !OPERATORS.contains(operator)) {
      throw new InvalidReportQueryException("Unknown operator: " + operator);
    }
  }

  public static void requireField(String field) {
    if (!NAMES.contains(field)) {
      throw new InvalidReportQueryException("Unknown field: " + field);
    }
  }
}
