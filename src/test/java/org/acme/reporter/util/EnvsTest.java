package org.acme.reporter.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EnvsTest {

  @Test
  void resolveUsesTheFirstPresentName() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("server.port", "9090");
    environment.setProperty("local.server.port", "9091");

    assertThat(Envs.resolve(environment, "8080", "local.server.port", "server.port"))
        .isEqualTo("9091");
  }

  @Test
  void resolveSkipsAMissingName() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("server.port", "9090");

    assertThat(Envs.resolve(environment, "8080", "local.server.port", "server.port"))
        .isEqualTo("9090");
  }

  @Test
  void resolveUsesTheFallbackWhenEveryNameIsBlank() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("server.port", " ");

    assertThat(Envs.resolve(environment, "8080", "local.server.port", "server.port"))
        .isEqualTo("8080");
  }
}
