import { updateReport } from "../api/reports.js";
import { changedBody } from "../ui/report-form.js";

const form = document.querySelector("#edit-form");
const error = document.querySelector("#edit-error");
const submit = form?.querySelector("button[type=submit]");
const reportPage = "/reports/" + encodeURIComponent(form?.dataset.id ?? "");

form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    error.hidden = true;
    // Only changed fields are sent; with none, there is nothing to save.
    const changes = changedBody(form);
    if (Object.keys(changes).length === 0) {
        window.location.assign(reportPage);
        return;
    }
    submit.disabled = true;
    try {
        await updateReport(form.dataset.id, changes);
        window.location.assign(reportPage);
    } catch (requestError) {
        error.hidden = false;
        error.textContent = requestError.message;
        submit.disabled = false;
    }
});
