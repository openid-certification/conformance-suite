import { html, nothing } from "lit";

/**
 * Render a test-author description string as a Lit template, restoring two
 * affordances the legacy `fapi.ui.js` block had and the Lit redesign dropped:
 *
 *   - paragraph breaks on `\n\n` (split into `<p>` blocks)
 *   - inline-code spans wrapped in `` ` `` render as `<code>`
 *
 * Origin: brief at docs/plans/2026-05-22-001-mr1998-maintainer-feedback-brief.md
 * (C2, Thomas + almgren follow-up, screenshots 07/08).
 *
 * Intentionally NOT a Markdown subset — only the two transforms above are
 * supported. Unbalanced backticks render literally because the matcher
 * requires a closing backtick. CRLF inputs are normalised to LF.
 *
 * @param {string | null | undefined} text - The raw description (already
 *   trimmed by `splitTestSummary` when piped through `cts-test-summary`).
 * @returns {ReturnType<typeof html> | typeof nothing | Array<ReturnType<typeof html>>}
 *   A Lit-renderable value. Empty / non-string inputs return `nothing`.
 */
export function formatDescription(text) {
  if (typeof text !== "string" || text.length === 0) return nothing;
  const normalized = text.replace(/\r\n/g, "\n");
  const paragraphs = normalized.split(/\n{2,}/).filter((p) => p.length > 0);
  if (paragraphs.length === 0) return nothing;
  return paragraphs.map((paragraph) => html`<p>${renderInlineCode(paragraph)}</p>`);
}

const INLINE_CODE_RE = /`([^`]+)`/g;

function renderInlineCode(text) {
  const parts = [];
  let lastIndex = 0;
  for (const match of text.matchAll(INLINE_CODE_RE)) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    parts.push(html`<code>${match[1]}</code>`);
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }
  return parts;
}
