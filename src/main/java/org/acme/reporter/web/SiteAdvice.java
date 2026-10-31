package org.acme.reporter.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.acme.reporter.config.ReporterProperties;
import org.acme.reporter.config.SwaggerUiConfig;

@ControllerAdvice
@RequiredArgsConstructor
public class SiteAdvice {

  private final ReporterProperties properties;
  private final SwaggerUiConfig swagger;

  @ModelAttribute("github")
  public String github() {
    return properties.github();
  }

  @ModelAttribute("swagger")
  public String swagger() {
    return swagger.path();
  }
}
