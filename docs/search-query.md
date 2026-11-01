# Search query

The reports page parses text in the browser and posts a JSON tree to `POST /api/reports/search`. Page and sort stay on the query string. An empty box calls `GET /api/reports` with the same page parameters.

```text
title = Login AND author = 'john.doe'
```

Clauses use `field operator value`. Operators are `=`, `!=`, `~`, `!~`, `>`, `>=`, `<`, `<=`, and `IN`. Combine them with `AND`, `OR`, `NOT`, and parentheses. Quote values that contain a dot or a space. `IN` takes a parenthesized list.

```json
{
  "type": "and",
  "children": [
    { "type": "clause", "field": "title", "operator": "=", "value": "Login" },
    { "type": "clause", "field": "author", "operator": "=", "value": "john.doe" }
  ]
}
```

`~` is a case-insensitive contains match. `tags` matches membership and supports `=`, `!=`, `~`, `!~`, and `IN`.

- `status`, `priority`, and `type` compare in declared order, so `priority > LOW` means `MEDIUM`, `HIGH`, and `CRITICAL`. Status runs `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.
- `createdOn` and `updatedOn` take an instant (`2026-01-15T09:30:00Z`) or a date (`2026-01-15`). A date with `=`, `!=`, or `IN` matches any time on that UTC day. With a range operator it means the start of that day.
- Range operators (`>`, `>=`, `<`, `<=`) take a single value and never match a report whose field is empty. `!=` and `!~` do match empty fields.

An unknown field, operator, enum name, or unparseable date is a 400, whatever reports are stored. See [pagination and sorting](pagination-and-sorting.md).
