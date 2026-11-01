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
