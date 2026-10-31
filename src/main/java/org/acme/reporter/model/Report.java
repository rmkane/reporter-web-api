package org.acme.reporter.model;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record Report(
    @Schema(example = "RPT-1") String id,
    @Schema(example = "Login") String title,
    @Schema(example = "The sign-in form accepts the demo password and still returns an error.")
        String description,
    @Schema(example = "OPEN") ReportStatus status,
    List<String> tags,
    @Schema(example = "john.doe") String author,
    @Schema(example = "sam.patel") String assignee,
    @Schema(example = "HIGH") Priority priority,
    @Schema(example = "BUG") ReportType type,
    @Schema(example = "2026-01-06T16:00:00Z") Instant createdOn,
    @Schema(example = "2026-01-08T18:00:00Z") Instant updatedOn) {}
