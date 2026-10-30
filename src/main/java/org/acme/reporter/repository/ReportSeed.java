package org.acme.reporter.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.model.Report;

@Component
public class ReportSeed {

  private final JsonMapper jsonMapper;
  private final String location;

  public ReportSeed(JsonMapper jsonMapper, @Value("${reporter.seed.location}") String location) {
    this.jsonMapper = jsonMapper;
    this.location = location;
  }

  public List<Report> load() {
    try (InputStream input = new DefaultResourceLoader().getResource(location).getInputStream()) {
      return jsonMapper.readValue(input, new TypeReference<List<Report>>() {});
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read seed file: " + location, ex);
    }
  }
}
