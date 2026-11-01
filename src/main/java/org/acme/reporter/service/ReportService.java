package org.acme.reporter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.acme.reporter.model.Report;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.web.CreateReportRequest;
import org.acme.reporter.web.UpdateReportRequest;

public interface ReportService {

  Page<Report> list(Pageable pageable);

  Report get(String id);

  Page<Report> search(ReportQuery query, Pageable pageable);

  Report create(CreateReportRequest request);

  /** Updates the report; a non-null {@code expectedVersion} must match its current version. */
  Report update(String id, UpdateReportRequest request, String expectedVersion);

  void delete(String id);
}
