package org.acme.reporter.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import org.acme.reporter.model.Report;
import org.acme.reporter.query.ReportQueries;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.repository.PageSlice;
import org.acme.reporter.repository.ReportRepository;
import org.acme.reporter.repository.ReportSeed;
import org.acme.reporter.repository.ReportSorts;

@Slf4j
@Repository
public class ReportRepositoryImpl implements ReportRepository {

  private final ConcurrentMap<String, Report> reports = new ConcurrentHashMap<>();

  @Autowired
  public ReportRepositoryImpl(ReportSeed seed) {
    this(seed.load());
    log.debug("Loaded {} reports from the seed file", reports.size());
  }

  public ReportRepositoryImpl(List<Report> initial) {
    for (Report report : initial) {
      if (reports.putIfAbsent(report.id(), report) != null) {
        throw new IllegalStateException("Duplicate report id: " + report.id());
      }
    }
  }

  @Override
  public Optional<Report> findById(String id) {
    log.debug("Query findById id={}", id);
    return Optional.ofNullable(reports.get(id));
  }

  @Override
  public Page<Report> findAll(Pageable pageable) {
    log.debug("Query findAll pageable={}", pageable);
    return PageSlice.slice(
        List.copyOf(reports.values()), pageable, ReportSorts.of(pageable.getSort()));
  }

  @Override
  public Report save(Report entity) {
    reports.put(entity.id(), entity);
    return entity;
  }

  @Override
  public void deleteById(String id) {
    reports.remove(id);
  }

  @Override
  public long count() {
    log.debug("Query count");
    return reports.size();
  }

  @Override
  public Page<Report> search(ReportQuery query, Pageable pageable) {
    log.debug("Query search query={} pageable={}", query, pageable);
    List<Report> matched =
        reports.values().stream().filter(report -> ReportQueries.matches(report, query)).toList();
    return PageSlice.slice(matched, pageable, ReportSorts.of(pageable.getSort()));
  }
}
