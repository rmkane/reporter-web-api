package org.acme.reporter.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PathsTest {

  @Test
  void ensureLeadingSlashPrefixesABarePath() {
    assertThat(Paths.ensureLeadingSlash("swagger-ui/index.html"))
        .isEqualTo("/swagger-ui/index.html");
  }

  @Test
  void ensureLeadingSlashKeepsAnAbsolutePath() {
    assertThat(Paths.ensureLeadingSlash("/v3/api-docs")).isEqualTo("/v3/api-docs");
  }
}
