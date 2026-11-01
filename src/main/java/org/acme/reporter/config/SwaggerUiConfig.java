package org.acme.reporter.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import org.acme.reporter.util.Paths;

@ConfigurationProperties(prefix = "springdoc.swagger-ui")
@Validated
public record SwaggerUiConfig(@NotBlank @DefaultValue(DEFAULT_PATH) String path) {

  public static final String DEFAULT_PATH = "/swagger-ui/index.html";

  public SwaggerUiConfig {
    path = Paths.ensureLeadingSlash(path);
  }
}
