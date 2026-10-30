import { fetchReports } from "../api/reports.js";
import { parseQuery } from "../query/parse.js";
import { renderReports } from "../ui/render.js";

const state = { page: 0, size: 10, sort: "updatedOn,desc", text: "" };
const error = document.querySelector("#query-error");
const input = document.querySelector("#query");

document.querySelector("#search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    state.text = input.value.trim();
    state.page = 0;
    load();
});

load();

async function load() {
    error.hidden = true;
    let query = null;
    if (state.text) {
        try {
            query = parseQuery(state.text);
        } catch (parseError) {
            show(parseError.message);
            return;
        }
    }
    try {
        const page = await fetchReports({ query, page: state.page, size: state.size, sort: state.sort });
        renderReports(page, state.sort, changeSort, (next) => {
            state.page = next;
            load();
        });
    } catch (requestError) {
        show(requestError.message);
    }
}

function changeSort(field) {
    const [current, direction] = state.sort.split(",");
    state.sort = field + "," + (current === field && direction === "asc" ? "desc" : "asc");
    state.page = 0;
    load();
}

function show(message) {
    error.hidden = false;
    error.textContent = message;
}
