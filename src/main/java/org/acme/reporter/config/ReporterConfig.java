package org.acme.reporter.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class ReporterConfig {

  @Bean
  public ReporterProperties.Seed seed(ReporterProperties properties) {
    return properties.seed();
  }
}
