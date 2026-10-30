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

`~` is a case-insensitive contains match. `tags` matches membership. An unknown field or operator is a 400. See [pagination and sorting](pagination-and-sorting.md).
