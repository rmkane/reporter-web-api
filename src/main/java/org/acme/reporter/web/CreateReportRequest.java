package org.acme.reporter.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

public record CreateReportRequest(
    @NotBlank @Schema(example = "Nightly digest is late") String title,
    @NotBlank @Schema(example = "The Monday mail arrives after noon.") String description,
    ReportStatus status,
    List<@NotBlank String> tags,
    @NotBlank @Schema(example = "jane.roe") String author,
    @Size(min = 1) String assignee,
    @Schema(example = "HIGH") Priority priority,
    @Schema(example = "BUG") ReportType type) {}
