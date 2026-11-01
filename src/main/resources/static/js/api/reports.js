export async function fetchReports({ query, page, size, sort }) {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort });
    const search = query != null;
    const response = await fetch((search ? "/api/reports/search?" : "/api/reports?") + params, search ? {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify(query)
    } : { headers: { "Accept": "application/json" } });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(body.message || "Request failed");
    }
    return body;
}

export async function fetchReport(id) {
    // no-store: this read checks the version, so it must never come from a cache.
    const response = await fetch("/api/reports/" + encodeURIComponent(id), { cache: "no-store", headers: { "Accept": "application/json" } });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(body.message || "Could not load the report");
    }
    return body;
}

export function createReport(report) {
    return sendReport("/api/reports", "POST", report, "Could not create the report");
}

// Thrown when the report changed since the page loaded it; carries the report as it is now.
export class ReportConflict extends Error {
    constructor(message, current) {
        super(message);
        this.current = current;
    }
}

// Sends the loaded version as If-Match, so the server refuses to overwrite newer changes.
export function updateReport(id, changes, version) {
    const headers = version ? { "If-Match": '"' + version + '"' } : {};
    return sendReport("/api/reports/" + encodeURIComponent(id), "PATCH", changes, "Could not save the report", headers);
}

async function sendReport(url, method, payload, fallback, extraHeaders = {}) {
    const response = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", "Accept": "application/json", ...extraHeaders },
        body: JSON.stringify(payload)
    });
    const body = await response.json().catch(() => ({}));
    if (response.status === 409 && body.current) {
        throw new ReportConflict(body.message, body.current);
    }
    if (!response.ok) {
        const details = (body.errors || [])
            .filter((error) => error.field)
            .map((error) => error.field + ": " + error.message)
            .join(" ");
        throw new Error(details || body.message || fallback);
    }
    return body;
}

export async function deleteReport(id) {
    const response = await fetch("/api/reports/" + encodeURIComponent(id), { method: "DELETE" });
    if (!response.ok && response.status !== 204) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || "Delete failed");
    }
}
