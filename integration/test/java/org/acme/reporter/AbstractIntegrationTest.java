package org.acme.reporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public abstract class AbstractIntegrationTest {

  protected static final Path OUTPUT_PATH = Path.of("target", "integration");

  private static final Environment ENVIRONMENT = new StandardEnvironment();

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @LocalServerPort private int localPort;

  @Autowired private JsonMapper jsonMapper;

  private RestClient client;

  @BeforeEach
  void bindClient() {
    client =
        RestClient.builder()
            .baseUrl(baseUrl())
            .requestInterceptor(
                (request, body, execution) -> {
                  log.info("{} {}", request.getMethod(), request.getURI());
                  return execution.execute(request, body);
                })
            .configureMessageConverters(
                converters ->
                    converters.withJsonConverter(new JacksonJsonHttpMessageConverter(mapper())))
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
            .build();
  }

  protected String baseUrl() {
    return "http://" + host() + ":" + port();
  }

  protected String host() {
    return property("reporter.integration.host", "localhost");
  }

  protected int port() {
    if (localPort > 0) {
      return localPort;
    }
    return Integer.parseInt(property("reporter.integration.port", "8080"));
  }

  protected String endpoint(String path) {
    String prefix = contextPath();
    if (prefix.isEmpty()) {
      return path;
    }
    int query = path.indexOf('?');
    String resource = query < 0 ? path : path.substring(0, query);
    String queryString = query < 0 ? "" : path.substring(query);
    if (!resource.startsWith("/")) {
      resource = "/" + resource;
    }
    return prefix + resource + queryString;
  }

  private static String contextPath() {
    String path = property("reporter.integration.path", "");
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    String prefixed = path.startsWith("/") ? path : "/" + path;
    return prefixed.endsWith("/") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;
  }

  private static String property(String key, String fallback) {
    String value = ENVIRONMENT.getProperty(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value.trim();
  }

  protected JsonMapper mapper() {
    return jsonMapper == null ? JSON : jsonMapper;
  }

  protected RestClient client() {
    return client;
  }

  protected JsonNode tree(String json) {
    return mapper().readTree(json);
  }

  protected void saveJson(String name, String json) throws IOException {
    Files.createDirectories(OUTPUT_PATH);
    String formatted =
        mapper().writerWithDefaultPrettyPrinter().writeValueAsString(mapper().readTree(json));
    Files.writeString(OUTPUT_PATH.resolve(name), formatted + System.lineSeparator());
  }

  protected static List<String> ids(JsonNode page) {
    return page.path("content").valueStream().map(node -> node.path("id").stringValue()).toList();
  }

  protected String get(String path) {
    return get(path, HttpStatus.OK.value()).body();
  }

  protected Call get(String path, int status) {
    Call call = send(client.get().uri(endpoint(path)));
    assertThat(call.status()).isEqualTo(status);
    return call;
  }

  protected <T> ResponseEntity<T> get(String path, Class<T> type) {
    return client.get().uri(endpoint(path)).retrieve().toEntity(type);
  }

  protected <T> ResponseEntity<T> get(String path, ParameterizedTypeReference<T> type) {
    return client.get().uri(endpoint(path)).retrieve().toEntity(type);
  }

  protected String post(String path, String json) {
    return post(path, json, HttpStatus.OK.value()).body();
  }

  protected Call post(String path, String json, int status) {
    return sendJson(client.post().uri(endpoint(path)), json, status);
  }

  protected <T> ResponseEntity<T> post(String path, Object body, Class<T> type) {
    return client
        .post()
        .uri(endpoint(path))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toEntity(type);
  }

  protected <T> ResponseEntity<T> post(
      String path, Object body, ParameterizedTypeReference<T> type) {
    return client
        .post()
        .uri(endpoint(path))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toEntity(type);
  }

  protected Call patch(String path, String json, int status) {
    return sendJson(client.patch().uri(endpoint(path)), json, status);
  }

  protected <T> ResponseEntity<T> patch(String path, Object body, Class<T> type) {
    return client
        .patch()
        .uri(endpoint(path))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toEntity(type);
  }

  protected ResponseEntity<Void> delete(String path) {
    return client.delete().uri(endpoint(path)).retrieve().toBodilessEntity();
  }

  protected HttpStatusCode status(HttpMethod method, String path) {
    return client
        .method(method)
        .uri(endpoint(path))
        .exchange(
            (request, response) -> {
              try (InputStream body = response.getBody()) {
                body.transferTo(OutputStream.nullOutputStream());
              }
              return response.getStatusCode();
            });
  }

  private Call sendJson(RestClient.RequestBodySpec request, String json, int status) {
    Call call =
        send(
            request
                .contentType(MediaType.APPLICATION_JSON)
                .body(outputStream -> outputStream.write(json.getBytes(StandardCharsets.UTF_8))));
    assertThat(call.status()).isEqualTo(status);
    return call;
  }

  private Call send(RestClient.RequestHeadersSpec<?> request) {
    return request.exchange(AbstractIntegrationTest::read);
  }

  private static Call read(HttpRequest request, ClientHttpResponse response) throws IOException {
    return new Call(
        response.getStatusCode().value(),
        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
  }

  protected record Call(int status, String body) {}
}
