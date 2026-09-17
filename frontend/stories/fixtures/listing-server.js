/**
 * A mocked listing endpoint that behaves like the server's `PaginationRequest`
 * paging, so a story can assert what a page of `/api/log` or `/api/plan` shows
 * rather than only which URL was requested.
 *
 * TWIN: `frontend/e2e/helpers/listing-server.js` is the same function for
 * the Playwright specs; keep the two in step.
 *
 * What it emulates, per request parameter:
 * - `search`: a case-insensitive substring match over `searchFields`. The
 *   real server runs a MongoDB `$text` phrase, which matches whole words only;
 *   the substring is a superset of that, which is all a UI spec needs.
 * - the parameters named in `filters`: comma-separated lists, matched
 *   case-insensitively against the named row field (`status=running,waiting`).
 * - `order`: a flat `column,direction,...` list; strings compare with
 *   `localeCompare`, which orders ISO timestamps correctly.
 * - `start` / `length`: the page.
 *
 * The answer is the DataTables envelope with the server's SYNTHETIC total:
 * `start + length + 1` when a further page exists, otherwise `start +
 * data.length`, so the listing learns "there is more", never "how many".
 * @module listing-server
 */

/**
 * @param {ReadonlyArray<Record<string, unknown>>} rows - The whole dataset the endpoint holds.
 * @param {string} requestUrl - The request URL, with its query string.
 * @param {object} [options] - How to interpret the request.
 * @param {ReadonlyArray<string>} [options.searchFields] - Row fields `search` matches against.
 * @param {Record<string, string>} [options.filters] - Request parameter → row field for the
 *   comma-list filters.
 * @returns {{draw: number, recordsTotal: number, recordsFiltered: number, data: Array<Record<string, unknown>>}}
 *   The page, in the paging envelope.
 */
export function serveListing(rows, requestUrl, { searchFields = [], filters = {} } = {}) {
  const params = new URL(requestUrl).searchParams;
  let matched = [...rows];

  const term = (params.get("search") || "").trim().toLowerCase();
  if (term) {
    matched = matched.filter((row) =>
      searchFields.some((field) =>
        String(row[field] ?? "")
          .toLowerCase()
          .includes(term),
      ),
    );
  }

  for (const [param, field] of Object.entries(filters)) {
    const raw = params.get(param);
    if (!raw) continue;
    const wanted = new Set(
      raw
        .split(",")
        .map((value) => value.trim().toUpperCase())
        .filter(Boolean),
    );
    matched = matched.filter((row) => wanted.has(String(row[field] ?? "").toUpperCase()));
  }

  const orderParts = (params.get("order") || "").split(",");
  /** @type {Array<{column: string, desc: boolean}>} */
  const keys = [];
  for (let i = 0; i < orderParts.length; i += 2) {
    const column = orderParts[i].trim();
    if (!column) continue;
    keys.push({ column, desc: (orderParts[i + 1] || "").trim() === "desc" });
  }
  if (keys.length > 0) {
    matched.sort((a, b) => {
      for (const { column, desc } of keys) {
        const order = String(a[column] ?? "").localeCompare(String(b[column] ?? ""));
        if (order !== 0) return desc ? -order : order;
      }
      return 0;
    });
  }

  const start = parseInt(params.get("start") || "0", 10);
  const length = parseInt(params.get("length") || "10", 10);
  const page = matched.slice(start, start + length);
  const total = matched.length > start + length ? start + length + 1 : start + page.length;

  return {
    draw: parseInt(params.get("draw") || "1", 10),
    recordsTotal: total,
    recordsFiltered: total,
    data: page,
  };
}

/** The fields `/api/log` rows are searched on, as the logs listing's mock. */
export const LOG_SEARCH_FIELDS = ["testName", "testId", "description", "planId", "planName"];

/** The comma-list filters `/api/log` takes. */
export const LOG_FILTERS = { status: "status", result: "result" };

/** The fields `/api/plan` rows are searched on, as the plans listing's mock. */
export const PLAN_SEARCH_FIELDS = ["planName", "_id", "description"];
