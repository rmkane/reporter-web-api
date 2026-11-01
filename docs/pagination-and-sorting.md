# Pagination and sorting

List and search take Spring Data `Pageable` arguments.

| Parameter | Meaning |
| --- | --- |
| `page` | Zero-based page index. Defaults to 0. |
| `size` | Page size. Defaults to 10 and is capped at 50. |
| `sort` | `property,asc` or `property,desc`. Repeat the parameter to sort by more than one property. |

The default sort is `updatedOn,desc`. An unknown property is a 400. `status`, `priority`, and `type` sort in declared order, so `priority,desc` puts `CRITICAL` first. `id` compares its number, so `RPT-2` precedes `RPT-10`. Empty values sort last in both directions.

The JSON body is a Spring Data page: `content`, plus `page.size`, `page.number`, `page.totalElements`, and `page.totalPages`. Column headers on the reports page change `sort` and return to page 0.

See [search query](search-query.md).
