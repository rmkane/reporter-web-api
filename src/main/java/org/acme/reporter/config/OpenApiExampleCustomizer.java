package org.acme.reporter.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import tools.jackson.databind.json.JsonMapper;

import org.acme.reporter.web.ReportController;

@Component
@RequiredArgsConstructor
class OpenApiExampleCustomizer implements OperationCustomizer, OpenApiCustomizer {

  private static final String JSON = "application/json";

  private static final String LOGIN_REPORT =
      """
      {
        "id": "RPT-1",
        "title": "Login",
        "description": "The sign-in form accepts the demo password and still returns an error.",
        "status": "OPEN",
        "tags": ["login", "auth"],
        "author": "john.doe",
        "assignee": "sam.patel",
        "priority": "HIGH",
        "type": "BUG",
        "createdOn": "2026-01-06T16:00:00Z",
        "updatedOn": "2026-01-08T18:00:00Z"
      }
      """;

  private static final String LOGIN_PAGE =
      """
      {
        "content": [%s],
        "page": { "size": 10, "number": 0, "totalElements": 30, "totalPages": 3 }
      }
      """
          .formatted(LOGIN_REPORT);

  private static final String SEARCH_PAGE =
      """
      {
        "content": [%s],
        "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
      }
      """
          .formatted(LOGIN_REPORT);

  private static final String TITLE_AND_AUTHOR =
      """
      {
        "type": "and",
        "children": [
          { "type": "clause", "field": "title", "operator": "=", "value": "Login" },
          { "type": "clause", "field": "author", "operator": "=", "value": "john.doe" }
        ]
      }
      """;

  private static final String HIGH_OR_CRITICAL =
      """
      {
        "type": "clause",
        "field": "priority",
        "operator": "IN",
        "value": ["HIGH", "CRITICAL"]
      }
      """;

  private static final String CREATE =
      """
      {
        "title": "Nightly digest is late",
        "description": "The Monday mail arrives after noon.",
        "tags": ["email", "digest"],
        "author": "jane.roe",
        "priority": "HIGH",
        "type": "BUG"
      }
      """;

  private static final String CREATED =
      """
      {
        "id": "RPT-31",
        "title": "Nightly digest is late",
        "description": "The Monday mail arrives after noon.",
        "status": "OPEN",
        "tags": ["email", "digest"],
        "author": "jane.roe",
        "priority": "HIGH",
        "type": "BUG",
        "createdOn": "2026-10-30T21:00:00Z",
        "updatedOn": "2026-10-30T21:00:00Z"
      }
      """;

  private static final String PARTIAL_UPDATE =
      """
      {
        "status": "IN_PROGRESS",
        "assignee": "alex.kim"
      }
      """;

  private static final String UPDATED =
      """
      {
        "id": "RPT-1",
        "title": "Login",
        "description": "The sign-in form accepts the demo password and still returns an error.",
        "status": "IN_PROGRESS",
        "tags": ["login", "auth"],
        "author": "john.doe",
        "assignee": "alex.kim",
        "priority": "HIGH",
        "type": "BUG",
        "createdOn": "2026-01-06T16:00:00Z",
        "updatedOn": "2026-10-30T21:00:00Z"
      }
      """;

  private static final String MISSING =
      """
      {
        "status": 404,
        "message": "Report RPT-99 was not found",
        "errors": []
      }
      """;

  private static final String INVALID =
      """
      {
        "status": 400,
        "message": "Validation failed",
        "errors": [{ "field": "title", "message": "must not be blank" }]
      }
      """;

  private static final String UNKNOWN_FIELD =
      """
      {
        "status": 400,
        "message": "Unknown field: owner",
        "errors": []
      }
      """;

  private final JsonMapper jsonMapper;

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }
    var schemas = openApi.getComponents().getSchemas();
    propertyExample(schemas.get("Report"), "tags", List.of("login", "auth"));
    propertyExample(schemas.get("CreateReportRequest"), "tags", List.of("email", "digest"));
  }

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    if (handlerMethod.getBeanType() != ReportController.class) {
      return operation;
    }
    idExample(operation);
    pageExamples(operation);
    return switch (handlerMethod.getMethod().getName()) {
      case "list" -> responseExample(operation, "200", "First page", LOGIN_PAGE);
      case "search" -> search(operation);
      case "get" -> missing(responseExample(operation, "200", "Login", LOGIN_REPORT));
      case "create" -> create(operation);
      case "update" -> update(operation);
      case "delete" -> missing(operation);
      default -> operation;
    };
  }

  private Operation search(Operation operation) {
    requestExample(operation, "Title and author", TITLE_AND_AUTHOR);
    requestExample(operation, "High or critical", HIGH_OR_CRITICAL);
    error(operation, "400", "Unknown field", UNKNOWN_FIELD);
    return responseExample(operation, "200", "Login by john.doe", SEARCH_PAGE);
  }

  private Operation create(Operation operation) {
    requestExample(operation, "Bug", CREATE);
    error(operation, "400", "Validation failed", INVALID);
    move(operation, "200", "201", "Created");
    return responseExample(operation, "201", "Created", CREATED);
  }

  private Operation update(Operation operation) {
    requestExample(operation, "Status and assignee", PARTIAL_UPDATE);
    error(operation, "400", "Validation failed", INVALID);
    return missing(responseExample(operation, "200", "Updated", UPDATED));
  }

  private void idExample(Operation operation) {
    parameterExample(operation, "id", "RPT-1");
  }

  /**
   * Sort is one {@code property,direction} value. An array example is sent as JSON and Spring
   * rejects it.
   */
  private void pageExamples(Operation operation) {
    parameterExample(operation, "page", 0);
    parameterExample(operation, "size", 10);
    schemaDefault(operation, "size", 10);
    if (operation.getParameters() == null) {
      return;
    }
    for (Parameter parameter : operation.getParameters()) {
      if ("sort".equals(parameter.getName())) {
        parameter.setExample("updatedOn,desc");
        parameter.setSchema(new Schema<String>().type("string").example("updatedOn,desc"));
      }
    }
  }

  private void schemaDefault(Operation operation, String name, Object value) {
    if (operation.getParameters() == null) {
      return;
    }
    for (Parameter parameter : operation.getParameters()) {
      if (name.equals(parameter.getName()) && parameter.getSchema() != null) {
        parameter.getSchema().setDefault(value);
      }
    }
  }

  private void parameterExample(Operation operation, String name, Object value) {
    if (operation.getParameters() == null) {
      return;
    }
    for (Parameter parameter : operation.getParameters()) {
      if (name.equals(parameter.getName())) {
        parameter.setExample(value);
      }
    }
  }

  private void requestExample(Operation operation, String summary, String json) {
    RequestBody body = operation.getRequestBody();
    if (body == null) {
      return;
    }
    addExample(jsonMedia(body.getContent()), summary, json);
  }

  private Operation responseExample(
      Operation operation, String status, String summary, String json) {
    ApiResponse response = operation.getResponses().get(status);
    if (response != null && response.getContent() != null) {
      response.getContent().values().forEach(media -> addExample(media, summary, json));
    }
    return operation;
  }

  private void move(Operation operation, String from, String to, String description) {
    ApiResponses responses = operation.getResponses();
    if (responses.get(to) == null && responses.get(from) != null) {
      ApiResponse response = responses.remove(from);
      response.setDescription(description);
      responses.addApiResponse(to, response);
    }
  }

  private static void propertyExample(Schema<?> schema, String name, Object value) {
    if (schema == null || schema.getProperties() == null) {
      return;
    }
    Schema<?> property = schema.getProperties().get(name);
    if (property != null) {
      property.setExample(value);
    }
  }

  private Operation missing(Operation operation) {
    return error(operation, "404", "Missing report", MISSING);
  }

  private Operation error(Operation operation, String status, String summary, String json) {
    ApiResponses responses = operation.getResponses();
    ApiResponse response = responses.get(status);
    if (response == null) {
      response = new ApiResponse().description(summary);
      responses.addApiResponse(status, response);
    }
    if (response.getContent() == null) {
      response.setContent(new Content());
    }
    MediaType media = response.getContent().computeIfAbsent(JSON, key -> new MediaType());
    addExample(media, summary, json);
    return operation;
  }

  private void addExample(MediaType media, String summary, String json) {
    if (media == null) {
      return;
    }
    media.addExamples(
        summary, new Example().summary(summary).value(jsonMapper.readValue(json, Object.class)));
  }

  private static MediaType jsonMedia(Content content) {
    return content == null ? null : content.get(JSON);
  }
}
