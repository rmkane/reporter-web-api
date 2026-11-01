package org.acme.reporter.repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.function.Function;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.model.Report;

/** Turns a Spring {@link Sort} into a {@link Report} comparator. */
@UtilityClass
public class ReportSorts {

  /** Unsorted requests fall back to id order. */
  public Comparator<Report> of(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return by(Report::id, ReportSorts::compareId, false);
    }
    Comparator<Report> combined = null;
    for (Sort.Order order : sort) {
      Comparator<Report> next = comparing(order.getProperty(), order.isDescending());
      combined = combined == null ? next : combined.thenComparing(next);
    }
    return combined;
  }

  /** Enums sort by declared order, so priority runs LOW to CRITICAL. Nulls stay last either way. */
  private Comparator<Report> comparing(String property, boolean descending) {
    Comparator<String> text = String.CASE_INSENSITIVE_ORDER;
    return switch (property) {
      case "id" -> by(Report::id, ReportSorts::compareId, descending);
      case "title" -> by(Report::title, text, descending);
      case "description" -> by(Report::description, text, descending);
      case "status" -> by(Report::status, Comparator.naturalOrder(), descending);
      case "tags" ->
          by(
              report -> report.tags() == null ? "" : String.join(",", report.tags()),
              text,
              descending);
      case "author" -> by(Report::author, text, descending);
      case "assignee" -> by(Report::assignee, text, descending);
      case "priority" -> by(Report::priority, Comparator.naturalOrder(), descending);
      case "type" -> by(Report::type, Comparator.naturalOrder(), descending);
      case "createdOn" -> by(Report::createdOn, Comparator.<Instant>naturalOrder(), descending);
      case "updatedOn" -> by(Report::updatedOn, Comparator.<Instant>naturalOrder(), descending);
      default -> throw new InvalidReportQueryException("Unknown sort property: " + property);
    };
  }

  private <T> Comparator<Report> by(
      Function<Report, T> key, Comparator<T> order, boolean descending) {
    return Comparator.comparing(key, Comparator.nullsLast(descending ? order.reversed() : order));
  }

  /**
   * Text runs stay alphabetical. Digit runs compare as integers, so {@code RPT-2} precedes {@code
   * RPT-10}.
   */
  private int compareId(String left, String right) {
    int leftIndex = 0;
    int rightIndex = 0;
    while (leftIndex < left.length() && rightIndex < right.length()) {
      char leftChar = left.charAt(leftIndex);
      char rightChar = right.charAt(rightIndex);
      if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
        int leftStart = leftIndex;
        int rightStart = rightIndex;
        while (leftIndex < left.length() && Character.isDigit(left.charAt(leftIndex))) {
          leftIndex++;
        }
        while (rightIndex < right.length() && Character.isDigit(right.charAt(rightIndex))) {
          rightIndex++;
        }
        int compared =
            compareNumber(
                left.substring(leftStart, leftIndex), right.substring(rightStart, rightIndex));
        if (compared != 0) {
          return compared;
        }
      } else {
        int compared =
            Character.compare(Character.toUpperCase(leftChar), Character.toUpperCase(rightChar));
        if (compared != 0) {
          return compared;
        }
        leftIndex++;
        rightIndex++;
      }
    }
    return Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
  }

  private int compareNumber(String left, String right) {
    int leftIndex = 0;
    int rightIndex = 0;
    while (leftIndex < left.length() - 1 && left.charAt(leftIndex) == '0') {
      leftIndex++;
    }
    while (rightIndex < right.length() - 1 && right.charAt(rightIndex) == '0') {
      rightIndex++;
    }
    int leftLength = left.length() - leftIndex;
    int rightLength = right.length() - rightIndex;
    if (leftLength != rightLength) {
      return Integer.compare(leftLength, rightLength);
    }
    return left.substring(leftIndex).compareTo(right.substring(rightIndex));
  }
}
