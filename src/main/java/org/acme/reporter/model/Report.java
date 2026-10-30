package org.acme.reporter.model;

import java.time.Instant;
import java.util.List;

public record Report(
    String id,
    String title,
    String description,
    ReportStatus status,
    List<String> tags,
    String author,
    String assignee,
    Priority priority,
    ReportType type,
    Instant createdOn,
    Instant updatedOn) {}
