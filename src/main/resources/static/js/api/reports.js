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

export async function createReport(report) {
    const response = await fetch("/api/reports", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify(report)
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        const details = (body.errors || [])
            .filter((error) => error.field)
            .map((error) => error.field + ": " + error.message)
            .join(" ");
        throw new Error(details || body.message || "Could not create the report");
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
