package org.acme.reporter.error;

public class ReportNotFoundException extends RuntimeException {

  public ReportNotFoundException(String id) {
    super("Report " + id + " was not found");
  }
}
