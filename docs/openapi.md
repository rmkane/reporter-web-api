# OpenAPI

`OpenApiConfig` publishes one `OpenAPI` bean titled **Reporter Web API**, version **0.1.0**. springdoc serves that document at `/v3/api-docs` and the Swagger UI at `/swagger-ui/index.html`. Override those paths with `springdoc.api-docs.path` and `springdoc.swagger-ui.path`.

The API controller adds an operation summary on each route. There is one OpenAPI bean. See [setup](setup.md).
