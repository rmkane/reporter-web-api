import { createReport } from "../api/reports.js";
import { createBody } from "../ui/report-form.js";

const form = document.querySelector("#create-form");
const error = document.querySelector("#create-error");

form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    error.hidden = true;
    try {
        const created = await createReport(createBody(form));
        window.location.assign("/reports/" + encodeURIComponent(created.id));
    } catch (requestError) {
        error.hidden = false;
        error.textContent = requestError.message;
    }
});
