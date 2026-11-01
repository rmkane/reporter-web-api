package org.acme.reporter.error;

import lombok.Getter;

import org.acme.reporter.model.Report;

/** An update that names a version the report has since moved past. */
@Getter
public class ReportConflictException extends RuntimeException {

  private final transient Report current;

  public ReportConflictException(Report current) {
    super("Report " + current.id() + " changed since it was loaded");
    this.current = current;
  }
}
