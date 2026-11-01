package org.acme.reporter.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.config.ReporterProperties.Seed;
import org.acme.reporter.model.Report;

class ReportSeedTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void loadsTheBundledSeedFile() {
    List<Report> reports =
        new ReportSeed(jsonMapper, new Seed("classpath:data/reports.json")).load();

    assertThat(reports).isNotEmpty();
    assertThat(reports).allSatisfy(report -> assertThat(report.id()).matches("RPT-\\d+"));
    assertThat(new HashSet<>(reports.stream().map(Report::id).toList())).hasSameSizeAs(reports);
  }

  @Test
  void missingSeedFileFailsWithItsLocation() {
    ReportSeed seed = new ReportSeed(jsonMapper, new Seed("classpath:data/missing.json"));

    assertThatThrownBy(seed::load)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Could not read seed file: classpath:data/missing.json");
  }
}
