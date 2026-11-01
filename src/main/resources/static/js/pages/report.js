import { deleteReport } from "../api/reports.js";

// Deleting cannot be undone, so it goes through a dialog that only enables
// the delete button once the report id is typed. Cancel has focus first.
const openButton = document.querySelector("#delete-report");
const dialog = document.querySelector("#delete-dialog");
const form = document.querySelector("#delete-form");
const input = document.querySelector("#delete-confirm");
const cancel = document.querySelector("#delete-cancel");
const submit = document.querySelector("#delete-submit");
const error = document.querySelector("#delete-error");
const id = dialog?.dataset.id;

openButton?.addEventListener("click", () => {
    input.value = "";
    submit.disabled = true;
    error.hidden = true;
    dialog.showModal();
});

input?.addEventListener("input", () => {
    submit.disabled = !confirmed();
});

cancel?.addEventListener("click", () => dialog.close());

// A click on the backdrop lands on the dialog element itself.
dialog?.addEventListener("click", (event) => {
    if (event.target === dialog) {
        dialog.close();
    }
});

form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!confirmed()) {
        return;
    }
    error.hidden = true;
    setBusy(true);
    try {
        await deleteReport(id);
        window.location.assign("/reports");
    } catch (requestError) {
        error.hidden = false;
        error.textContent = requestError.message;
        setBusy(false);
    }
});

function confirmed() {
    return input.value.trim() === id;
}

function setBusy(busy) {
    submit.disabled = busy || !confirmed();
    cancel.disabled = busy;
    input.disabled = busy;
    submit.textContent = busy ? "Deleting…" : "Delete report";
}
