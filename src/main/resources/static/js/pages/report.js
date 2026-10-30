import { deleteReport } from "../api/reports.js";

const button = document.querySelector("#delete-report");
const error = document.querySelector("#delete-error");

button?.addEventListener("click", async () => {
    error.hidden = true;
    try {
        await deleteReport(button.dataset.id);
        window.location.assign("/reports");
    } catch (requestError) {
        error.hidden = false;
        error.textContent = requestError.message;
    }
});
