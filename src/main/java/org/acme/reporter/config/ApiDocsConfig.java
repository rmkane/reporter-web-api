package org.acme.reporter.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import org.acme.reporter.util.Paths;

@Validated
@ConfigurationProperties(prefix = "springdoc.api-docs")
public record ApiDocsConfig(@NotBlank @DefaultValue(DEFAULT_PATH) String path) {

  public static final String DEFAULT_PATH = "/v3/api-docs";

  public ApiDocsConfig {
    path = Paths.ensureLeadingSlash(path);
  }
}
