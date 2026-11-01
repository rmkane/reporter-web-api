// Formats ISO timestamps for people, in their own locale and time zone. The
// raw value stays in the <time datetime> attribute; sorting uses the API's
// raw field, not this text.
const display = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" });
const detail = new Intl.DateTimeFormat(undefined, { dateStyle: "full", timeStyle: "long" });

export function timeElement(iso) {
    const element = document.createElement("time");
    if (iso) {
        element.dateTime = iso;
        localize(element);
    }
    return element;
}

// Upgrades server-rendered <time datetime> elements in place.
export function localizeTimes(root = document) {
    root.querySelectorAll("time[datetime]").forEach(localize);
}

function localize(element) {
    const date = new Date(element.dateTime);
    if (Number.isNaN(date.getTime())) {
        return;
    }
    element.textContent = display.format(date);
    element.title = detail.format(date);
}
