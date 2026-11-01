# Create and update

`Report` is the stored record. Clients send a smaller record.

`POST /api/reports` requires `title`, `description`, and `author`. Status, tags, assignee, priority, and type are optional. A missing status becomes `OPEN`. The server assigns the next `RPT-n` id and both timestamps. The response is 201.

`PATCH /api/reports/{id}` accepts the same fields, all optional. A null field leaves the stored value. A blank or whitespace-only string is rejected, and text is trimmed. `updatedOn` is refreshed.

`DELETE /api/reports/{id}` returns 204. A missing id on get, update, or delete is 404.

Validation errors use one `ApiError` body: `status`, `message`, and `errors`. See [architecture](architecture.md).
