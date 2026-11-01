package org.acme.reporter.model;

import lombok.experimental.UtilityClass;

/** A report's version is its updatedOn instant, which every save moves forward. */
@UtilityClass
public class ReportVersion {

  public String of(Report report) {
    return String.valueOf(report.updatedOn());
  }

  /**
   * The version in an If-Match header, without quotes or a weak prefix. Returns null when the
   * header is absent or "*", which means the client does not ask for a version check.
   */
  public String fromIfMatch(String header) {
    if (header == null || header.isBlank() || header.strip().equals("*")) {
      return null;
    }
    String value = header.strip();
    if (value.startsWith("W/")) {
      value = value.substring(2);
    }
    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
      value = value.substring(1, value.length() - 1);
    }
    return value;
  }
}
