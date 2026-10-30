package org.acme.reporter.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

public record CreateReportRequest(
    @NotBlank String title,
    @NotBlank String description,
    ReportStatus status,
    List<@NotBlank String> tags,
    @NotBlank String author,
    @Size(min = 1) String assignee,
    Priority priority,
    ReportType type) {}
