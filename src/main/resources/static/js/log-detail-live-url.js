/**
 * Parse a `log-detail.html?log=<id>` page URL into the test id and the
 * matching `/api/info/<id>` backend URL.
 *
 * Mirrors the URL→endpoint mapping `js/log-detail.js` performs on page load
 * (`?log=<id>` → `GET /api/info/<id>`). Used by the cts-log-detail-header
 * "LiveBackend" Storybook story so a developer can paste the URL they are
 * looking at and have the story fetch + inject the real test info.
 *
 * The returned `apiUrl` carries the pasted URL's origin, so the fetch targets
 * the same backend the developer is viewing rather than Storybook's origin.
 *
 * @param {string} logUrl - A full `…/log-detail.html?log=<id>` URL.
 * @returns {{ id: string, apiUrl: string }}
 * @throws {Error} with a UI-facing message when the input is blank, not a
 *   valid URL, or missing the `log` query parameter.
 */
export function parseLogUrl(logUrl) {
  if (typeof logUrl !== "string" || logUrl.trim() === "") {
    throw new Error("Paste a log-detail.html?log=<id> URL.");
  }

  let url;
  try {
    url = new URL(logUrl.trim());
  } catch {
    throw new Error("That doesn't look like a URL.");
  }

  const id = url.searchParams.get("log");
  if (!id) {
    throw new Error("URL is missing the ?log=<id> parameter.");
  }

  const apiUrl = `${url.origin}/api/info/${encodeURIComponent(id)}`;
  return { id, apiUrl };
}
