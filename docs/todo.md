# To do

## Edit safety follow-ups

Edits from the edit page cannot silently overwrite another person's change: `PATCH` carries `If-Match` and the server checks the version and saves under one lock (see [create and update](create-and-update.md#concurrent-edits)). These gaps remain, in suggested order.

### 1. Cache static files correctly

The packaged jar serves CSS and JS with `Last-Modified: Fri, 01 Feb 1980` (Maven's reproducible build timestamp) and no `Cache-Control`. Browsers derive a heuristic freshness of years from that date, so after a deploy users can keep old CSS and JS, including an `edit.js` that predates `If-Match` and could overwrite a same-field change.

- Set `spring.web.resources.cache.use-last-modified=false` and `spring.web.resources.cache.cachecontrol.no-cache=true`.
- Add a content-based `ETag` for static resources, for example a `ShallowEtagHeaderFilter` on `/css/*`, `/js/*`, and the favicons, so unchanged files still answer 304.
- Done when: a changed `app.css` reaches a browser on a normal reload after redeploying the jar, and an unchanged one comes back as 304.

### 2. Check the version on delete

`DELETE` has no version check, so deleting a report can discard an edit someone saved after the page loaded. The confirm dialog makes deleting deliberate, but not informed.

- Accept `If-Match` on `DELETE /api/reports/{id}` and answer 409 with the current report when it is stale, as `PATCH` does.
- Have the report page send its version, and on a 409 show what changed in the dialog before allowing the delete.
- Done when: deleting from a page loaded before someone else's save shows their change and requires confirming again.

### 3. Require If-Match on PATCH

`PATCH` without `If-Match` still updates unconditionally, for existing API clients. Any client that skips the header can overwrite newer changes.

- Answer `428 Precondition Required` when `PATCH` has no `If-Match`.
- Breaking change for API clients: update `LocalhostReportIntegrationTest`, `ReportApiIntegrationTest`, and the OpenAPI description, and mark it `BREAKING CHANGE` in the commit and changelog.
- Done when: every update path sends a version, and an unversioned `PATCH` is rejected.

### Known limits, not planned

- Field-level merging cannot see dependencies between fields: closing a report while someone raises its priority applies both. Treating every concurrent save as a conflict would catch it at the cost of constant notices.
- `GET /api/reports/{id}` reads through a service cache, and a read racing a save can put the old report back into it. The worst case is a needless conflict notice or a briefly stale page, never a lost write, since saves check the live store.
