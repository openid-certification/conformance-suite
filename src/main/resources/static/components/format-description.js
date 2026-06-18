import { nothing } from "lit";
import { unsafeHTML } from "lit/directives/unsafe-html.js";
import { marked } from "marked";
import DOMPurify from "dompurify";

// Module-load configuration (runs once, before any consumer's first render).
// GFM gives bare-URL autolinking; single `\n` stays a soft break (not <br>),
// matching how these summaries use `\n\n` for paragraph separation. These are
// marked's v18 defaults — set explicitly so a future default change can't
// silently alter rendering.
marked.setOptions({ gfm: true, breaks: false });

// Force every sanitized link to open in a new tab without leaking the opener.
// Global to the DOMPurify singleton (this module is its only consumer); the
// guard makes it a no-op on non-anchor nodes.
DOMPurify.addHook("afterSanitizeAttributes", (/** @type {Element} */ node) => {
  if (node.tagName === "A" && node.hasAttribute("href")) {
    node.setAttribute("target", "_blank");
    node.setAttribute("rel", "noopener noreferrer");
  }
});

/**
 * Render a test-author description string (markdown) to sanitized HTML for a
 * Lit child binding.
 *
 * Test descriptions are authored as markdown in `@PublishTestModule(summary=…)`
 * and plan definitions: paragraphs, bullet lists, inline `` `code` ``, and bare
 * `http`/`https` URLs that should be clickable. We render them with `marked`
 * (GFM enabled so bare URLs autolink) and sanitize the result with `DOMPurify`
 * before handing it to Lit's `unsafeHTML`.
 *
 * Why a real markdown library rather than a bespoke formatter: the prose is
 * dense with snake_case identifiers (`access_token`, `claims_supported`). A
 * naive 1-KB parser mangles those into emphasis (`access<em>token</em>`);
 * marked follows CommonMark's intraword-underscore rule and leaves them intact
 * (verified empirically). marked is also one of the fastest parsers and ships a
 * single self-contained ESM file, so it vendors cleanly with no build step.
 *
 * Safety: `marked` does not sanitize, so its output is always passed through
 * `DOMPurify` before `unsafeHTML`. An `afterSanitizeAttributes` hook forces
 * every surviving link to `target="_blank" rel="noopener noreferrer"`.
 *
 * Origin: docs/plans/2026-05-27-001-feat-autolink-and-format-test-prose-plan.md
 * (supersedes the bespoke paragraph/code formatter from MR-1998 C2).
 *
 * @param {string | null | undefined} text - The raw markdown description
 *   (already split from instructions by `splitTestSummary` when piped through
 *   `cts-test-summary`, so the `\n\n---\n\n` marker never reaches marked).
 * @returns {ReturnType<typeof unsafeHTML> | typeof nothing} A Lit-renderable
 *   value. Empty / non-string inputs return `nothing`.
 */
export function formatDescription(text) {
  if (typeof text !== "string" || text.trim().length === 0) return nothing;
  const dirty = /** @type {string} */ (marked.parse(text.replace(/\r\n/g, "\n")));
  return unsafeHTML(DOMPurify.sanitize(dirty));
}

/**
 * Render a markdown summary as a single inline run, safe to nest inside an
 * interactive element (the `cts-test-selector` plan rows are `<button>`s, so
 * block elements and anchors are invalid there).
 *
 * Uses `marked.parseInline` (no `<p>` / `<ul>` wrappers) over whitespace-
 * collapsed text, then sanitizes with a positive phrasing-only allowlist. Only
 * inline `<code>`/`<em>`/`<strong>` can survive — never an `<a>`, `<img>`,
 * `<button>`, or other interactive/embedded element that would be invalid
 * nested inside the row `<button>`. A positive allowlist (rather than forbidding
 * `<a>` alone) keeps the teaser button-safe for any future markdown construct.
 * Disallowed tags are unwrapped to their text, so an autolinked URL degrades to
 * plain text.
 *
 * @param {string | null | undefined} text - Raw markdown summary.
 * @returns {ReturnType<typeof unsafeHTML> | typeof nothing} A Lit-renderable
 *   value. Empty / non-string inputs return `nothing`.
 */
export function formatSummaryPreview(text) {
  if (typeof text !== "string" || text.trim().length === 0) return nothing;
  const collapsed = text.replace(/\s+/g, " ").trim();
  const dirty = /** @type {string} */ (marked.parseInline(collapsed));
  return unsafeHTML(
    DOMPurify.sanitize(dirty, {
      ALLOWED_TAGS: ["code", "em", "strong", "b", "i", "del"],
      ALLOWED_ATTR: [],
    }),
  );
}
