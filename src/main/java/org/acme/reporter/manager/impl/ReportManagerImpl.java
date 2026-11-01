package org.acme.reporter.manager.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.acme.reporter.error.ReportConflictException;
import org.acme.reporter.error.ReportNotFoundException;
import org.acme.reporter.manager.ReportManager;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportVersion;
import org.acme.reporter.query.ReportFields;
import org.acme.reporter.query.ReportQueries;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.repository.ReportRepository;
import org.acme.reporter.web.CreateReportRequest;
import org.acme.reporter.web.UpdateReportRequest;

@Service
@RequiredArgsConstructor
public class ReportManagerImpl implements ReportManager {

  private static final Pattern REPORT_ID = Pattern.compile("RPT-(\\d+)");

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

  // Synchronized so two creates cannot pick the same id and overwrite each other.
  @Override
  public synchronized Report create(CreateReportRequest request) {
    Instant now = Instant.now();
    Report report =
        new Report(
            nextId(),
            request.title().trim(),
            request.description().trim(),
            keep(request.status(), ReportStatus.OPEN),
            keep(request.tags(), List.of()),
            request.author().trim(),
            blankToNull(request.assignee()),
            request.priority(),
            request.type(),
            now,
            now);
    return repository.save(report);
  }

  // Synchronized with create and the checked update, so a version check and its save are atomic.
  @Override
  public synchronized Report update(
      String id, UpdateReportRequest request, String expectedVersion) {
    Report current = repository.findById(id).orElseThrow(() -> new ReportNotFoundException(id));
    if (expectedVersion != null && !expectedVersion.equals(ReportVersion.of(current))) {
      throw new ReportConflictException(current);
    }
    return update(current, request);
  }

  @Override
  public synchronized Report update(Report current, UpdateReportRequest request) {
    Report updated =
        new Report(
            current.id(),
            keep(request.title(), current.title()),
            keep(request.description(), current.description()),
            keep(request.status(), current.status()),
            keep(request.tags(), current.tags()),
            keep(request.author(), current.author()),
            keep(request.assignee(), current.assignee()),
            keep(request.priority(), current.priority()),
            keep(request.type(), current.type()),
            current.createdOn(),
            nextUpdatedOn(current));
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
            .filter(id -> id != null)
            .map(REPORT_ID::matcher)
            .filter(Matcher::matches)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max()
            .orElse(0);
    return "RPT-" + (max + 1);
  }

  /** Always later than the current value, since updatedOn doubles as the report's version. */
  private static Instant nextUpdatedOn(Report current) {
    Instant now = Instant.now();
    Instant previous = current.updatedOn();
    return previous == null || now.isAfter(previous) ? now : previous.plusNanos(1);
  }

  private static <T> T keep(T incoming, T current) {
    return incoming == null ? current : incoming;
  }

  private static String keep(String incoming, String current) {
    return incoming == null ? current : incoming.trim();
  }

  private static List<String> keep(List<String> incoming, List<String> current) {
    return incoming == null ? current : List.copyOf(incoming);
  }

  private static String blankToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
