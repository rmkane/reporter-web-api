package org.acme.reporter.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

public record UpdateReportRequest(
    @Size(min = 1) String title,
    @Size(min = 1) String description,
    ReportStatus status,
    List<@NotBlank String> tags,
    @Size(min = 1) String author,
    @Size(min = 1) String assignee,
    Priority priority,
    ReportType type) {}
