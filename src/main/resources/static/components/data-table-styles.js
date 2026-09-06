import { css } from "lit";

/**
 * The one look every accessible data table on the statistics page shares: the
 * `<details>` that discloses it and the `<table>` inside. `<cts-chart>`,
 * `<cts-heatmap>`, the insights section and the page's own unresolved-plans
 * table all render one, and each used to carry its own copy of these rules so
 * that it looked right on a page with none of the others — which also meant
 * the page's table only looked right because a chart had injected its
 * stylesheet first. Now each of them injects this once instead. Spacing stays
 * with the component: where a table sits is its business, what it looks like
 * is not.
 *
 * @module data-table-styles
 */

/*
 * The id is the disclosure's, not the table's: `cts-data-table` (the paged
 * DataTables replacement) injects its own sheet under
 * `cts-data-table-styles`, and two sheets sharing an id means whichever
 * mounts first suppresses the other entirely.
 */
const STYLE_ID = "cts-data-disclosure-styles";

const STYLE_TEXT = css`
  .cts-data-disclosure > summary {
    cursor: pointer;
    font-size: var(--fs-13, 13px);
    color: var(--fg-muted);
  }
  .cts-data-disclosure > summary:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-2, 4px);
  }
  .cts-data-table {
    width: 100%;
    margin-top: var(--space-2, 8px);
    border-collapse: collapse;
    font-size: var(--fs-13, 13px);
    /* Columns of numbers align; see the data-viz figures rule. */
    font-variant-numeric: tabular-nums;
  }
  .cts-data-table caption {
    text-align: left;
    padding-bottom: var(--space-2, 8px);
    font-size: var(--fs-12, 12px);
    color: var(--fg-soft);
  }
  .cts-data-table th,
  .cts-data-table td {
    padding: var(--space-2, 8px) var(--space-3, 12px);
    border-bottom: 1px solid var(--border);
    text-align: right;
  }
  .cts-data-table th[scope="col"]:first-child,
  .cts-data-table th[scope="row"] {
    text-align: left;
    font-weight: var(--fw-regular, 400);
  }
  .cts-data-table thead th {
    color: var(--fg-soft);
    font-weight: var(--fw-bold, 700);
  }
`;

/**
 * Append the shared data-table stylesheet to `<head>` once per page lifetime.
 * Every component that renders one calls this from its own `injectStyles`,
 * so whichever of them lands on a page first brings the rules with it.
 * @returns {void}
 */
export function injectDataTableStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}
