package org.acme.reporter.web;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.acme.reporter.model.Report;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.service.ReportService;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

  private final ReportService reports;

  public ReportController(ReportService reports) {
    this.reports = reports;
  }

  @GetMapping
  @Operation(summary = "List reports")
  @PageableAsQueryParam
  public Page<Report> list(
      @Parameter(hidden = true)
          @PageableDefault(size = 10, sort = "updatedOn", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return reports.list(pageable);
  }

  @PostMapping("/search")
  @Operation(summary = "Search reports")
  @PageableAsQueryParam
  public Page<Report> search(
      @RequestBody ReportQuery query,
      @Parameter(hidden = true)
          @PageableDefault(size = 10, sort = "updatedOn", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return reports.search(query, pageable);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a report")
  public Report get(@PathVariable String id) {
    return reports.get(id);
  }

  @PostMapping
  @Operation(summary = "Create a report")
  public ResponseEntity<Report> create(@Valid @RequestBody CreateReportRequest request) {
    Report report = reports.create(request);
    return ResponseEntity.created(URI.create("/api/reports/" + report.id())).body(report);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update a report")
  public Report update(@PathVariable String id, @Valid @RequestBody UpdateReportRequest request) {
    return reports.update(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a report")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    reports.delete(id);
    return ResponseEntity.noContent().build();
  }
}
