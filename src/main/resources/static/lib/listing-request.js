/**
 * One page of a server-paged listing (`GET /api/log`, `GET /api/plan`): how a
 * page is asked for, and how the answer is read.
 *
 * Both endpoints take the same `PaginationRequest` on the server: `start`
 * (0-based offset), `length` (page size, capped at 1000), `order` (a flat
 * `column,direction,...` list) and `search` (a MongoDB `$text` phrase). They
 * answer with the DataTables envelope `{ draw, recordsTotal, recordsFiltered,
 * data }`, where `recordsTotal` is SYNTHETIC: `start + length + 1` when a
 * further page exists, otherwise `start + data.length`. So a listing never
 * knows how many rows there are in all, only whether there is a next page,
 * and that is what {@link readListingPage} reports.
 *
 * Pure functions only — no DOM, no fetch.
 * @module listing-request
 */

/** Rows per page, and so per "Show more". */
export const PAGE_SIZE = 25;

/**
 * The query string for one page.
 * @param {object} page - What to ask for.
 * @param {number} [page.start] - 0-based offset of the first row; 0 by default.
 * @param {number} [page.length] - Rows to return; {@link PAGE_SIZE} by default.
 * @param {string} [page.order] - The server's `order` value, e.g. `started,desc`.
 * @param {string} [page.search] - A search term; blank means none.
 * @param {boolean} [page.isPublic] - Ask for the published listing.
 * @param {URLSearchParams} [page.extra] - Endpoint-specific parameters (the
 *   plan filters, the log status/result lists), appended as given.
 * @returns {URLSearchParams} The parameters, `start` and `length` first.
 */
export function listingParams({ start = 0, length = PAGE_SIZE, order, search, isPublic, extra }) {
  const params = new URLSearchParams();
  params.set("start", String(start));
  params.set("length", String(length));
  if (order) params.set("order", order);
  const term = typeof search === "string" ? search.trim() : "";
  if (term) params.set("search", term);
  if (isPublic) params.set("public", "true");
  if (extra) {
    for (const [key, value] of extra.entries()) params.set(key, value);
  }
  return params;
}

/**
 * The rows of one page, and whether another follows it.
 *
 * Accepts the envelope the server sends and a plain array (test doubles and
 * stories). A plain array carries no total and is treated as complete.
 * @param {unknown} payload - The parsed response body.
 * @param {number} start - The offset the page was asked for.
 * @returns {{rows: Array<object>, hasMore: boolean}} What it holds.
 */
export function readListingPage(payload, start) {
  const body = /** @type {{data?: unknown, recordsTotal?: unknown}|null} */ (
    payload && typeof payload === "object" ? payload : null
  );
  const rows = Array.isArray(payload)
    ? payload
    : Array.isArray(body?.data)
      ? /** @type {Array<object>} */ (body.data)
      : [];
  const total = typeof body?.recordsTotal === "number" ? body.recordsTotal : null;
  return { rows, hasMore: total !== null && total > start + rows.length };
}
