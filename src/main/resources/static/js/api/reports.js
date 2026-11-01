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

export function createReport(report) {
    return sendReport("/api/reports", "POST", report, "Could not create the report");
}

export function updateReport(id, changes) {
    return sendReport("/api/reports/" + encodeURIComponent(id), "PATCH", changes, "Could not save the report");
}

async function sendReport(url, method, payload, fallback) {
    const response = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify(payload)
    });
    const body = await response.json().catch(() => ({}));
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
