package org.acme.reporter.web;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportVersion;
import org.acme.reporter.query.ReportQuery;
import org.acme.reporter.service.ReportService;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reports;

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
  public ResponseEntity<Report> get(@PathVariable String id) {
    return withVersion(reports.get(id));
  }

  @PostMapping
  @Operation(summary = "Create a report")
  public ResponseEntity<Report> create(@Valid @RequestBody CreateReportRequest request) {
    Report report = reports.create(request);
    return ResponseEntity.created(URI.create("/api/reports/" + report.id())).body(report);
  }

  @PatchMapping("/{id}")
  @Operation(
      summary = "Update a report",
      description =
          "Send the ETag from a GET as If-Match to update only an unchanged report. A stale"
              + " version is a 409 whose body holds the current report.")
  public ResponseEntity<Report> update(
      @PathVariable String id,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @Valid @RequestBody UpdateReportRequest request) {
    return withVersion(reports.update(id, request, ReportVersion.fromIfMatch(ifMatch)));
  }

  private static ResponseEntity<Report> withVersion(Report report) {
    return ResponseEntity.ok().eTag(ReportVersion.of(report)).body(report);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a report")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    reports.delete(id);
    return ResponseEntity.noContent().build();
  }
}
