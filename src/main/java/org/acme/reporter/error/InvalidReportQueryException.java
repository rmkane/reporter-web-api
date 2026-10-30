package org.acme.reporter.error;

public class InvalidReportQueryException extends RuntimeException {

  public InvalidReportQueryException(String message) {
    super(message);
  }
}
