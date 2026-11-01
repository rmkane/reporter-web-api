package org.acme.reporter.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;

public record UpdateReportRequest(
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String title,
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String description,
    ReportStatus status,
    List<@NotBlank String> tags,
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String author,
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String assignee,
    Priority priority,
    ReportType type) {}
