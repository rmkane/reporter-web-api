package org.acme.reporter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReporterConfig {

  @Bean
  public ReporterProperties.Seed seed(ReporterProperties properties) {
    return properties.seed();
  }
}
