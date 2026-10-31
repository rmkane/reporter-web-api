package org.acme.reporter.web;

public record Crumb(String label, String href) {

  public String getLabel() {
    return label;
  }

  public String getHref() {
    return href;
  }
}
