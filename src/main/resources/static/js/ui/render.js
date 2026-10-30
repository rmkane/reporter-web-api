export function renderReports(page, sort, onSort, onPage) {
    const rows = document.querySelector("#report-rows");
    const summary = document.querySelector("#page-summary");
    const pager = document.querySelector("#pager");
    const content = page.content || [];
    const meta = page.page || page;
    const number = meta.number ?? 0;
    const totalPages = meta.totalPages ?? 1;
    const total = meta.totalElements ?? content.length;

    summary.textContent = total === 0
        ? "No reports"
        : (number * (meta.size ?? content.length) + 1) + "–" + (number * (meta.size ?? content.length) + content.length) + " of " + total;

    document.querySelectorAll("button[data-sort]").forEach((button) => {
        const field = button.dataset.sort;
        button.onclick = () => onSort(field);
        if (sort.startsWith(field + ",")) {
            button.setAttribute("aria-sort", sort.endsWith(",asc") ? "ascending" : "descending");
        } else {
            button.removeAttribute("aria-sort");
        }
    });

    rows.replaceChildren(...content.map((report) => {
        const row = document.createElement("tr");
        row.append(
            cell(link(report.id, "/reports/" + encodeURIComponent(report.id))),
            cell(link(report.title, "/reports/" + encodeURIComponent(report.id))),
            cell(text(report.status)),
            cell(text(report.author)),
            cell(text(report.priority)),
            cell(text(formatWhen(report.updatedOn)))
        );
        return row;
    }));

    pager.replaceChildren();
    const previous = document.createElement("button");
    previous.type = "button";
    previous.textContent = "Previous";
    previous.disabled = number <= 0;
    previous.onclick = () => onPage(number - 1);
    const next = document.createElement("button");
    next.type = "button";
    next.textContent = "Next";
    next.disabled = number + 1 >= totalPages;
    next.onclick = () => onPage(number + 1);
    const label = document.createElement("span");
    label.textContent = "Page " + (number + 1) + " of " + Math.max(totalPages, 1);
    pager.append(previous, label, next);
}

function cell(node) {
    const item = document.createElement("td");
    item.append(node);
    return item;
}

function text(value) {
    return document.createTextNode(value == null ? "" : String(value));
}

function link(label, href) {
    const anchor = document.createElement("a");
    anchor.href = href;
    anchor.textContent = label == null ? "" : String(label);
    return anchor;
}

function formatWhen(value) {
    if (!value) {
        return "";
    }
    return String(value).replace("T", " ").replace("Z", " UTC");
}
