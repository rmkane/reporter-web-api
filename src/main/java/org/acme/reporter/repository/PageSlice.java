package org.acme.reporter.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.model.Report;

public final class PageSlice {

  private PageSlice() {}

  public static Page<Report> slice(List<Report> items, Pageable pageable) {
    List<Report> sorted = new ArrayList<>(items);
    Sort sort = pageable.getSort();
    if (sort.isSorted()) {
      sorted.sort(comparator(sort));
    } else {
      sorted.sort(Comparator.comparing(Report::id, ids()));
    }
    if (pageable.isUnpaged()) {
      return new PageImpl<>(List.copyOf(sorted));
    }
    int start = (int) Math.min(pageable.getOffset(), sorted.size());
    int end = Math.min(start + pageable.getPageSize(), sorted.size());
    return new PageImpl<>(List.copyOf(sorted.subList(start, end)), pageable, sorted.size());
  }

  private static Comparator<Report> comparator(Sort sort) {
    Comparator<Report> combined = null;
    for (Sort.Order order : sort) {
      Comparator<Report> next = comparing(order.getProperty());
      if (order.isDescending()) {
        next = next.reversed();
      }
      combined = combined == null ? next : combined.thenComparing(next);
    }
    return combined;
  }

  private static Comparator<Report> comparing(String property) {
    return switch (property) {
      case "id" -> Comparator.comparing(Report::id, ids());
      case "title" -> Comparator.comparing(Report::title, nulls());
      case "description" -> Comparator.comparing(Report::description, nulls());
      case "status" ->
          Comparator.comparing(
              report -> report.status() == null ? null : report.status().name(), nulls());
      case "tags" ->
          Comparator.comparing(
              report -> report.tags() == null ? "" : String.join(",", report.tags()), nulls());
      case "author" -> Comparator.comparing(Report::author, nulls());
      case "assignee" -> Comparator.comparing(Report::assignee, nulls());
      case "priority" ->
          Comparator.comparing(
              report -> report.priority() == null ? null : report.priority().name(), nulls());
      case "type" ->
          Comparator.comparing(
              report -> report.type() == null ? null : report.type().name(), nulls());
      case "createdOn" ->
          Comparator.comparing(
              Report::createdOn, Comparator.nullsLast(Comparator.<Instant>naturalOrder()));
      case "updatedOn" ->
          Comparator.comparing(
              Report::updatedOn, Comparator.nullsLast(Comparator.<Instant>naturalOrder()));
      default -> throw new InvalidReportQueryException("Unknown sort property: " + property);
    };
  }

  private static Comparator<String> nulls() {
    return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * Text runs stay alphabetical. Digit runs compare as integers, so {@code RPT-2} precedes {@code
   * RPT-10}.
   */
  private static Comparator<String> ids() {
    return Comparator.nullsLast(PageSlice::compareId);
  }

  private static int compareId(String left, String right) {
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

  private static int compareNumber(String left, String right) {
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
