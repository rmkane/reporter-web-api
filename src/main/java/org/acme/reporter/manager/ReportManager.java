package org.acme.reporter.manager;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.acme.reporter.model.Report;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.web.CreateReportRequest;
import org.acme.reporter.web.UpdateReportRequest;

public interface ReportManager {

  Page<Report> list(Pageable pageable);

  Optional<Report> find(String id);

  Page<Report> search(ReportQuery query, Pageable pageable);

  Report create(CreateReportRequest request);

  Report update(Report current, UpdateReportRequest request);

  /**
   * Updates the report when it is still at {@code expectedVersion}, or always when that is null.
   *
   * @throws org.acme.reporter.error.ReportConflictException when the report has moved on
   */
  Report update(String id, UpdateReportRequest request, String expectedVersion);

  void delete(String id);
}
