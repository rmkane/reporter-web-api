package org.acme.reporter.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

  private static final String REPORTS = "reports";
  private static final String REPORT_LISTS = "report-lists";
  private static final String REPORT_SEARCHES = "report-searches";

  private final ReportManager manager;

  @Override
  @Cacheable(REPORT_LISTS)
  public Page<Report> list(Pageable pageable) {
    return manager.list(pageable);
  }

  @Override
  @Cacheable(REPORTS)
  public Report get(String id) {
    return manager.find(id).orElseThrow(() -> new ReportNotFoundException(id));
  }

  @Override
  @Cacheable(REPORT_SEARCHES)
  public Page<Report> search(ReportQuery query, Pageable pageable) {
    return manager.search(query, pageable);
  }

  @Override
  @CacheEvict(
      cacheNames = {REPORTS, REPORT_LISTS, REPORT_SEARCHES},
      allEntries = true)
  public Report create(CreateReportRequest request) {
    return manager.create(request);
  }

  @Override
  @CacheEvict(
      cacheNames = {REPORTS, REPORT_LISTS, REPORT_SEARCHES},
      allEntries = true)
  public Report update(String id, UpdateReportRequest request, String expectedVersion) {
    return manager.update(id, request, expectedVersion);
  }

  @Override
  @CacheEvict(
      cacheNames = {REPORTS, REPORT_LISTS, REPORT_SEARCHES},
      allEntries = true)
  public void delete(String id) {
    if (manager.find(id).isEmpty()) {
      throw new ReportNotFoundException(id);
    }
    manager.delete(id);
  }
}
