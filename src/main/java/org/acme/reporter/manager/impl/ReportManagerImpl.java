package org.acme.reporter.manager.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.acme.reporter.manager.ReportManager;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.query.ReportFields;
import org.acme.reporter.query.ReportQueries;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.repository.ReportRepository;
import org.acme.reporter.web.CreateReportRequest;
import org.acme.reporter.web.UpdateReportRequest;

@Service
@RequiredArgsConstructor
public class ReportManagerImpl implements ReportManager {

  private final ReportRepository repository;

  @Override
  public Page<Report> list(Pageable pageable) {
    ReportFields.requireSort(pageable.getSort());
    return repository.findAll(pageable);
  }

  @Override
  public Optional<Report> find(String id) {
    return repository.findById(id);
  }

  @Override
  public Page<Report> search(ReportQuery query, Pageable pageable) {
    ReportQueries.validate(query);
    ReportFields.requireSort(pageable.getSort());
    return repository.search(query, pageable);
  }

  @Override
  public Report create(CreateReportRequest request) {
    Instant now = Instant.now();
    List<String> tags = request.tags() == null ? List.of() : List.copyOf(request.tags());
    Report report =
        new Report(
            nextId(),
            request.title().trim(),
            request.description().trim(),
            request.status() == null ? ReportStatus.OPEN : request.status(),
            tags,
            request.author().trim(),
            blankToNull(request.assignee()),
            request.priority(),
            request.type(),
            now,
            now);
    return repository.save(report);
  }

  @Override
  public Report update(Report current, UpdateReportRequest request) {
    Report updated =
        new Report(
            current.id(),
            keep(request.title(), current.title()),
            keep(request.description(), current.description()),
            request.status() == null ? current.status() : request.status(),
            request.tags() == null ? current.tags() : List.copyOf(request.tags()),
            keep(request.author(), current.author()),
            request.assignee() == null ? current.assignee() : request.assignee().trim(),
            request.priority() == null ? current.priority() : request.priority(),
            request.type() == null ? current.type() : request.type(),
            current.createdOn(),
            Instant.now());
    return repository.save(updated);
  }

  @Override
  public void delete(String id) {
    repository.deleteById(id);
  }

  private String nextId() {
    int max =
        repository.findAll(Pageable.unpaged()).getContent().stream()
            .map(Report::id)
            .filter(id -> id != null && id.startsWith("RPT-"))
            .mapToInt(id -> Integer.parseInt(id.substring(4)))
            .max()
            .orElse(0);
    return "RPT-" + (max + 1);
  }

  private static String keep(String incoming, String current) {
    return incoming == null ? current : incoming.trim();
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
