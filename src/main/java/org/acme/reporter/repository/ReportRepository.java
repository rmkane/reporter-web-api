package org.acme.reporter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.acme.reporter.model.Report;
import org.acme.reporter.query.ReportQuery;

public interface ReportRepository extends EntityRepository<Report, String> {

  Page<Report> search(ReportQuery query, Pageable pageable);
}
