// Reads the shared report form (templates/report-form.html). Text is trimmed,
// tags split on commas, and selects send their enum value.
const FIELDS = ["type", "title", "description", "tags", "author", "assignee", "status", "priority"];
const REQUIRED = new Set(["title", "description", "author"]);

// The fields to create a report with: required ones always, others when filled in.
export function createBody(form) {
    const body = {};
    for (const name of FIELDS) {
        const value = current(form, name);
        if (REQUIRED.has(name) || value !== "") {
            body[name] = payload(name, value);
        }
    }
    return body;
}

// The fields that differ from what the page loaded with, for a PATCH.
export function changedBody(form) {
    const body = {};
    for (const name of FIELDS) {
        const value = current(form, name);
        if (value !== initial(form, name)) {
            body[name] = payload(name, value);
        }
    }
    return body;
}

// After a 409: merges the report as it is now into the form. Fields only they changed take
// the latest value; fields only you changed keep yours; fields you both changed differently
// keep yours, marked with the latest value and a "Use latest" button. The latest values then
// become the baseline, so saving again sends just your remaining changes. onResolved runs
// when "Use latest" settles the last marked field. Returns the updated fields as
// { label, value } and the conflicting fields' labels.
export function reconcile(form, latest, onResolved = () => {}) {
    clearConflicts(form);
    const updated = [];
    const conflicts = [];
    for (const name of FIELDS) {
        const loaded = initial(form, name);
        const mine = current(form, name);
        const theirs = fromReport(name, latest);
        if (theirs !== loaded) {
            if (mine === loaded) {
                field(form, name).value = theirs;
                updated.push({ label: label(form, name), value: display(field(form, name), theirs) || "empty" });
            } else if (mine !== theirs) {
                markConflict(form, name, theirs, onResolved);
                conflicts.push(label(form, name));
            }
        }
        setBaseline(form, name, theirs);
    }
    return { updated, conflicts };
}

function fromReport(name, report) {
    const value = report[name];
    if (name === "tags") {
        return (value ?? []).join(", ");
    }
    return value == null ? "" : String(value);
}

function setBaseline(form, name, value) {
    const element = field(form, name);
    if (element instanceof HTMLSelectElement) {
        // Moving the default can move an untouched select, so keep the current choice.
        const chosen = element.value;
        for (const option of element.options) {
            option.defaultSelected = option.value === value;
        }
        // A set value cannot be cleared, so None goes away once there is one.
        if (value !== "") {
            element.querySelector('option[value=""]')?.remove();
        }
        element.value = chosen === "" && value !== "" ? value : chosen;
    } else {
        // Setting defaultValue on an edited input keeps what the user typed.
        const typed = element.value;
        element.defaultValue = value;
        element.value = typed;
    }
    if (name === "assignee") {
        element.required = value !== "";
    }
}

function markConflict(form, name, theirs, onResolved) {
    const element = field(form, name);
    const wrapper = element.parentElement;
    wrapper.classList.add("conflict");
    const note = document.createElement("p");
    note.className = "field-note";
    const latest = document.createElement("strong");
    latest.textContent = display(element, theirs) || "(empty)";
    const use = document.createElement("button");
    use.type = "button";
    use.className = "link";
    use.textContent = "Use latest";
    use.addEventListener("click", () => {
        element.value = theirs;
        wrapper.classList.remove("conflict");
        note.remove();
        if (!form.querySelector(".conflict")) {
            onResolved();
        }
    });
    note.append("Latest: ", latest, " ", use);
    wrapper.append(note);
}

function clearConflicts(form) {
    form.querySelectorAll(".field-note").forEach((note) => note.remove());
    form.querySelectorAll(".conflict").forEach((wrapper) => wrapper.classList.remove("conflict"));
}

function display(element, value) {
    if (element instanceof HTMLSelectElement) {
        return [...element.options].find((option) => option.value === value)?.textContent ?? value;
    }
    return value;
}

function label(form, name) {
    return form.querySelector(`label[for="${name}"]`)?.textContent.trim() ?? name;
}

function field(form, name) {
    return form.querySelector("#" + name);
}

function current(form, name) {
    return field(form, name).value.trim();
}

function initial(form, name) {
    const element = field(form, name);
    if (element instanceof HTMLSelectElement) {
        const selected = [...element.options].find((option) => option.defaultSelected) ?? element.options[0];
        return selected?.value ?? "";
    }
    return element.defaultValue.trim();
}

function payload(name, value) {
    if (name === "tags") {
        return value.split(",").map((tag) => tag.trim()).filter(Boolean);
    }
    return value;
}
