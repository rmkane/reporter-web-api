package org.acme.reporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.acme.reporter.config.ReporterProperties;

@SpringBootApplication
@EnableConfigurationProperties(ReporterProperties.class)
public class ReporterWebApp {

  public static void main(String[] args) {
    SpringApplication.run(ReporterWebApp.class, args);
  }
}
