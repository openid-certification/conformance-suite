import { css } from "lit";

/**
 * The two rules the statistics page and its insights section both need.
 *
 * `<cts-statistics-page>` and `<cts-statistics-insights>` are always mounted
 * together on the one page that uses either, and each used to declare its own
 * copy of the section heading and the small print under a table — identical
 * declarations under two names, so a change to the type scale had to be made
 * in both or the sections drifted apart. Spacing stays with the component:
 * where a heading or a hint sits is its business, what it looks like is not.
 *
 * The heading's `:first-child` reset is deliberately NOT here. It belongs to
 * the page, whose first heading opens the document; the insights section's
 * first heading follows the charts above it and needs its margin.
 *
 * @module statistics-styles
 */

const STYLE_ID = "cts-statistics-shared-styles";

const STYLE_TEXT = css`
  .cts-stats-section-heading {
    margin: var(--space-6) 0 var(--space-3);
    font-size: var(--fs-16);
    font-weight: var(--fw-bold);
    line-height: var(--lh-snug);
    color: var(--fg);
  }
  .cts-stats-hint {
    margin: var(--space-2) 0 0;
    font-size: var(--fs-12);
    color: var(--fg-soft);
  }
`;

/**
 * Append the shared statistics stylesheet to `<head>` once per page lifetime.
 * Both components call this from their own `injectStyles`, so whichever lands
 * first brings the rules with it.
 * @returns {void}
 */
export function injectStatisticsStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}
