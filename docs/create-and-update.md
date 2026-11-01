# Create and update

`Report` is the stored record. Clients send a smaller record.

`POST /api/reports` requires `title`, `description`, and `author`. Status, tags, assignee, priority, and type are optional. A missing status becomes `OPEN`. The server assigns the next `RPT-n` id and both timestamps. The response is 201.

`PATCH /api/reports/{id}` accepts the same fields, all optional. A null field leaves the stored value. A blank or whitespace-only string is rejected, and text is trimmed. `updatedOn` is refreshed.

In the UI, the report page's Edit button opens `/reports/{id}/edit`, the create form's fields filled in. It sends only the fields that changed. Because the API cannot clear a field, a field that already has a value can be changed but not emptied: its "None" option is hidden and a set assignee is required.

### Concurrent edits

`GET` and `PATCH` return the report's version as an `ETag`; the version is its `updatedOn`, which every save moves forward. Send it back as `If-Match` and the update only applies if nobody saved in between. Otherwise the response is 409, with the report as it is now in `current`:

```json
{ "status": 409, "message": "Report RPT-1 changed since it was loaded", "current": { "id": "RPT-1", "...": "..." } }
```

Without `If-Match`, `PATCH` updates unconditionally, as before. The edit page always sends it. On a 409 it fills in fields only the other person changed and keeps fields only you changed. If nobody changed the same field, it saves again against the new version without asking. If you both changed a field, it keeps your value, marks the field with the latest value and a "Use latest" button, and shows a notice until you resolve it or save again. Saving with no changes sends no `PATCH`, so the page checks the version with a `GET` instead: if the report changed, it stays put, shows the latest values, and says what changed. The check happens on save, so no live connection is needed.

`DELETE /api/reports/{id}` returns 204. A missing id on get, update, or delete is 404.

Validation errors use one `ApiError` body: `status`, `message`, and `errors`. See [architecture](architecture.md).
