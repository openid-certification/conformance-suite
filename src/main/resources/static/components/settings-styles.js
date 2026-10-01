import { css } from "lit";

const STYLE_ID = "cts-settings-styles";

/** Field, list, error-summary and action-row styles shared by the settings section components. */
const STYLE_TEXT = css`
  .cts-settings-form {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-settings-section {
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    padding: var(--space-4);
    margin-bottom: var(--space-4);
    background: var(--bg-elev, transparent);
  }
  .cts-settings-section h2 {
    font-size: var(--fs-18, 1.125rem);
    margin: 0 0 var(--space-2) 0;
  }
  .cts-settings-meta {
    font-size: var(--fs-13);
    color: var(--fg-muted);
    margin: 0 0 var(--space-3) 0;
  }
  .cts-settings-field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin-bottom: var(--space-3);
  }
  .cts-settings-field > label,
  .cts-settings-label {
    font-weight: 600;
    font-size: var(--fs-13);
  }
  .cts-settings-input {
    font: inherit;
    font-size: var(--fs-13);
    padding: var(--space-2);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    background: var(--bg, #fff);
    color: var(--fg);
  }
  .cts-settings-pem {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    resize: vertical;
  }
  .cts-settings-check {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
  }
  .cts-settings-error {
    margin: 0;
    color: var(--rust-500);
    font-size: var(--fs-13);
  }
  .cts-settings-keys {
    margin: 0 0 var(--space-2) 0;
    padding-left: var(--space-4);
    font-size: var(--fs-13);
  }
  .cts-settings-list {
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    margin: 0 0 var(--space-3) 0;
  }
  .cts-settings-list legend {
    font-weight: 600;
    padding: 0 var(--space-1);
  }
  .cts-settings-entry {
    border-top: 1px solid var(--border);
    padding-top: var(--space-3);
    margin-bottom: var(--space-3);
  }
  .cts-settings-cert-meta {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0 0 var(--space-3) 0;
    font-size: var(--fs-13);
  }
  .cts-settings-cert-meta dt {
    color: var(--fg-muted);
  }
  .cts-settings-cert-meta dd {
    margin: 0;
    overflow-wrap: anywhere;
  }
  .cts-settings-summary {
    border: 1px solid var(--rust-500);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    margin-bottom: var(--space-4);
    font-size: var(--fs-13);
  }
  .cts-settings-summary p,
  .cts-settings-summary ul {
    margin: 0;
  }
  .cts-settings-actions {
    display: flex;
    gap: var(--space-2);
  }
`;

/**
 * Inject the shared settings stylesheet into `<head>` once.
 * @returns {void}
 */
export function injectSettingsStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}
