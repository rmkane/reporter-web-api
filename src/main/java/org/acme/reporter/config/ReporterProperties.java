package org.acme.reporter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "reporter")
@Validated
public record ReporterProperties(@NotBlank String github, @NotNull @Valid Seed seed) {

  public record Seed(@NotBlank String location) {}
}
