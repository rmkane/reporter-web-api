package org.acme.reporter.query;

import java.util.Set;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;

import org.acme.reporter.error.InvalidReportQueryException;

@UtilityClass
public class ReportFields {

  public final Set<String> NAMES =
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

  private final Set<String> OPERATORS = Set.of("=", "!=", "~", "!~", ">", ">=", "<", "<=", "IN");

  public void requireSort(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return;
    }
    for (Sort.Order order : sort) {
      if (!NAMES.contains(order.getProperty())) {
        throw new InvalidReportQueryException("Unknown sort property: " + order.getProperty());
      }
    }
  }

  public void requireOperator(String operator) {
    if (operator == null || !OPERATORS.contains(operator)) {
      throw new InvalidReportQueryException("Unknown operator: " + operator);
    }
  }

  public void requireField(String field) {
    if (!NAMES.contains(field)) {
      throw new InvalidReportQueryException("Unknown field: " + field);
    }
  }
}
