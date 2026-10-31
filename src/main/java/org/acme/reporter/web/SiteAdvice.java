package org.acme.reporter.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.acme.reporter.config.ReporterProperties;

@ControllerAdvice
public class SiteAdvice {

  private final ReporterProperties properties;

  public SiteAdvice(ReporterProperties properties) {
    this.properties = properties;
  }

  @ModelAttribute("github")
  public String github() {
    return properties.github();
  }
}
