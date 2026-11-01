import { ReportConflict, fetchReport, updateReport } from "../api/reports.js";
import { changedBody, reconcile } from "../ui/report-form.js";

// Saves retried after merging someone else's changes, before asking the user instead.
const MAX_MERGED_RETRIES = 3;

const form = document.querySelector("#edit-form");
const error = document.querySelector("#edit-error");
const notice = document.querySelector("#edit-conflict");
const submit = form?.querySelector("button[type=submit]");
const reportPage = "/reports/" + encodeURIComponent(form?.dataset.id ?? "");

form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    error.hidden = true;
    submit.disabled = true;
    try {
        await save();
    } catch (requestError) {
        error.hidden = false;
        error.textContent = requestError.message;
    }
    submit.disabled = false;
});

// Sends only the changed fields, guarded by the version the form is based on. If someone
// saved in between without touching the same fields, their changes are merged into the form
// and the save is retried; only overlapping changes stop and ask the user.
async function save() {
    for (let attempt = 0; attempt <= MAX_MERGED_RETRIES; attempt++) {
        const changes = changedBody(form);
        if (Object.keys(changes).length === 0) {
            await leaveUnlessChangedElsewhere();
            return;
        }
        try {
            await updateReport(form.dataset.id, changes, form.dataset.version);
            window.location.assign(reportPage);
            return;
        } catch (requestError) {
            if (!(requestError instanceof ReportConflict)) {
                throw requestError;
            }
            const { updated, conflicts } = reconcile(form, requestError.current, hideNotice);
            form.dataset.version = requestError.current.updatedOn;
            if (conflicts.length || attempt === MAX_MERGED_RETRIES) {
                showNotice(updated, conflicts);
                return;
            }
        }
    }
}

// With nothing to send, no PATCH checks the version, so check it here: a form that looks
// unchanged may still show values someone else has since replaced.
async function leaveUnlessChangedElsewhere() {
    const latest = await fetchReport(form.dataset.id);
    if (latest.updatedOn === form.dataset.version) {
        window.location.assign(reportPage);
        return;
    }
    const { updated } = reconcile(form, latest, hideNotice);
    form.dataset.version = latest.updatedOn;
    const changes = updated.map(({ label, value }) => label + " is now " + value).join(", ");
    setNotice([
        "Nothing to save, but " + form.dataset.id + " was changed while you were editing" + (changes ? ": " + changes + "." : "."),
        "The form now shows the latest values."
    ]);
}

function showNotice(updated, conflicts) {
    const lines = [form.dataset.id + " was changed while you were editing."];
    if (updated.length) {
        lines.push("Updated to the latest: " + updated.map(({ label }) => label).join(", ") + ".");
    }
    if (conflicts.length) {
        lines.push("You both changed " + conflicts.join(", ") + ": your edits are kept and the latest values are shown below each field.");
    }
    lines.push("Review the form and save again.");
    setNotice(lines);
}

function setNotice(lines) {
    notice.replaceChildren(...lines.map((line) => {
        const paragraph = document.createElement("p");
        paragraph.textContent = line;
        return paragraph;
    }));
    notice.hidden = false;
    notice.scrollIntoView({ block: "nearest" });
}

function hideNotice() {
    notice.hidden = true;
}
