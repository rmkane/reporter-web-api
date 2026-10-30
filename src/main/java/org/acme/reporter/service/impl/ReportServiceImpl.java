package org.acme.reporter.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.acme.reporter.error.ReportNotFoundException;
import org.acme.reporter.manager.ReportManager;
import org.acme.reporter.model.Report;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.service.ReportService;
import org.acme.reporter.web.CreateReportRequest;
import org.acme.reporter.web.UpdateReportRequest;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

  private final ReportManager manager;

  @Override
  public Page<Report> list(Pageable pageable) {
    return manager.list(pageable);
  }

  @Override
  public Report get(String id) {
    return manager.find(id).orElseThrow(() -> new ReportNotFoundException(id));
  }

  @Override
  public Page<Report> search(ReportQuery query, Pageable pageable) {
    return manager.search(query, pageable);
  }

  @Override
  public Report create(CreateReportRequest request) {
    return manager.create(request);
  }

  @Override
  public Report update(String id, UpdateReportRequest request) {
    Report current = get(id);
    return manager.update(current, request);
  }

  @Override
  public void delete(String id) {
    get(id);
    manager.delete(id);
  }
}
