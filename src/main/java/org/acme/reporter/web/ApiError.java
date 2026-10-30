package org.acme.reporter.web;

import java.util.List;

public record ApiError(int status, String message, List<FieldError> errors) {

  public ApiError(int status, String message) {
    this(status, message, List.of());
  }

  public record FieldError(String field, String message) {}
}
