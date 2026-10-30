import { createReport } from "../api/reports.js";

const form = document.querySelector("#create-form");
const error = document.querySelector("#create-error");

form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    error.hidden = true;
    const report = {
        title: field("title"),
        description: field("description"),
        author: field("author")
    };
    copyOptional(report, "assignee");
    copyOptional(report, "status");
    copyOptional(report, "priority");
    copyOptional(report, "type");
    const tags = field("tags");
    if (tags) {
        report.tags = tags.split(",").map((tag) => tag.trim()).filter(Boolean);
    }
    try {
        const created = await createReport(report);
        window.location.assign("/reports/" + encodeURIComponent(created.id));
    } catch (requestError) {
        error.hidden = false;
        error.textContent = requestError.message;
    }
});

function field(id) {
    return document.querySelector("#" + id).value.trim();
}

function copyOptional(report, id) {
    const value = field(id);
    if (value) {
        report[id] = value;
    }
}
