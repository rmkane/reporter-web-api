package org.acme.reporter.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.config.ReporterProperties.Seed;
import org.acme.reporter.model.Report;

@Component
@RequiredArgsConstructor
public class ReportSeed {

  private final JsonMapper jsonMapper;
  private final Seed seed;

  public List<Report> load() {
    String location = seed.location();
    try (InputStream input = new DefaultResourceLoader().getResource(location).getInputStream()) {
      return jsonMapper.readValue(input, new TypeReference<>() {});
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read seed file: " + location, ex);
    }
  }
}
