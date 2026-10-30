package org.acme.reporter.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;
import org.acme.reporter.repository.impl.ReportRepositoryImpl;

class ReportRepositoryImplTest {

  @Test
  void findsSavesDeletesAndCounts() {
    Report original = sample("RPT-1");
    ReportRepositoryImpl repository = new ReportRepositoryImpl(new ArrayList<>(List.of(original)));

    assertThat(repository.findById("RPT-1")).contains(original);
    assertThat(repository.count()).isEqualTo(1);

    Report created = sample("RPT-2");
    repository.save(created);
    assertThat(repository.findAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);

    repository.deleteById("RPT-1");
    assertThat(repository.findById("RPT-1")).isEmpty();
    assertThat(repository.count()).isEqualTo(1);
  }

  private static Report sample(String id) {
    Instant now = Instant.parse("2026-04-01T00:00:00Z");
    return new Report(
        id,
        "Title",
        "Body",
        ReportStatus.OPEN,
        List.of("demo"),
        "john.doe",
        "alex.kim",
        Priority.LOW,
        ReportType.BUG,
        now,
        now);
  }
}
