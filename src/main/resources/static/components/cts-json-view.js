import { LitElement, html, css } from "lit";

/**
 * Lightweight, read-only JSON viewer. A deliberate alternative to a
 * read-only `<cts-json-editor>` (which boots the full Monaco engine):
 * this renders pretty-printed JSON as a syntax-highlighted `<pre>` with
 * zero dependencies and no editor runtime.
 *
 * Why this exists: instantiating a Monaco editor installs a page-wide
 * WebKit clipboard workaround (a `BrowserClipboardService` that opens a
 * pending `navigator.clipboard.write()` on every click to keep an
 * in-gesture write ready). WebKit allows only one outstanding async
 * clipboard write, so any *other* deferred `clipboard.write()` on the
 * page — e.g. the private-link auto-copy — is rejected with
 * `NotAllowedError`. Read-only JSON views never needed Monaco's editing
 * machinery; rendering them as a plain highlighted `<pre>` removes Monaco
 * from log/plan pages entirely, which both restores the auto-copy and is
 * a real page-weight win. Monaco stays for the genuinely-editable path
 * (`cts-config-form` on schedule-test).
 *
 * Drop-in for a read-only `<cts-json-editor>`: exposes `.value` as a
 * plain string and honours the same `min-height`/`max-height` sizing
 * contract (set them on the host; the view scrolls within those bounds).
 * Light DOM, so consumer CSS keyed on the host class (e.g. `.config-json`,
 * `.ctsConfigJson`) and selectors like `cts-json-view .oidf-json-view`
 * apply directly.
 * @property {string} value - JSON text to display. Mirrors
 *   `<cts-json-editor>.value` so call sites and tests reading `el.value`
 *   keep working after the swap.
 */
class CtsJsonView extends LitElement {
  static properties = {
    value: { type: String },
  };

  constructor() {
    super();
    /** @type {string} */
    this.value = "";
  }

  createRenderRoot() {
    injectStyles();
    return this;
  }

  /**
   * API parity with `cts-json-editor.whenReady()` so consumers/tests that
   * await readiness stay agnostic to which surface mounted. A static view
   * is ready as soon as it renders, so this resolves immediately.
   * @returns {Promise<{kind: "view", el: Element}>} Resolves with this host.
   */
  whenReady() {
    return Promise.resolve({ kind: "view", el: this });
  }

  render() {
    const segments = tokenizeJson(this.value || "");
    // Build the body separately and keep `<pre>…</pre>` on one template line:
    // a <pre> is whitespace-sensitive, so any literal newline the formatter
    // might inject around the interpolation would render as a blank line.
    const body = segments.map((s) =>
      s.cls ? html`<span class="${s.cls}">${s.text}</span>` : s.text,
    );
    return html`<pre class="oidf-json-view">${body}</pre>`;
  }
}

/**
 * Split pretty-printed JSON into highlight segments. Returns an array of
 * `{text, cls?}` covering the whole input (including the punctuation and
 * whitespace between tokens, emitted as classless gaps). The caller maps
 * each segment to a Lit `<span>` (or bare text), so token text is
 * auto-escaped by Lit — no `unsafeHTML`, no XSS even though config JSON
 * can contain arbitrary user strings.
 * @param {string} src - Pretty-printed JSON (already `JSON.stringify`-ed
 *   by callers). Non-JSON input simply yields fewer classified tokens.
 * @returns {Array<{text: string, cls?: string}>} Ordered, gap-inclusive
 *   segments. `cls` is one of `json-key|json-string|json-number|json-keyword`.
 */
function tokenizeJson(src) {
  /** @type {Array<{text: string, cls?: string}>} */
  const out = [];
  // Group 1: quoted string (without any trailing colon).
  // Group 2: optional `\s*:` ⇒ the string was an object key.
  // Group 3: literal keyword. Group 4: number.
  const re =
    /("(?:\\u[0-9a-fA-F]{4}|\\[^u]|[^\\"])*")(\s*:)?|\b(true|false|null)\b|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/g;
  let last = 0;
  let m;
  while ((m = re.exec(src)) !== null) {
    if (m.index > last) out.push({ text: src.slice(last, m.index) });
    if (m[1] !== undefined) {
      out.push({
        text: m[1],
        cls: m[2] !== undefined ? "json-key" : "json-string",
      });
      if (m[2] !== undefined) out.push({ text: m[2] }); // the ":" (+ ws) is punctuation
    } else if (m[3] !== undefined) {
      out.push({ text: m[3], cls: "json-keyword" });
    } else if (m[4] !== undefined) {
      out.push({ text: m[4], cls: "json-number" });
    }
    last = re.lastIndex;
  }
  if (last < src.length) out.push({ text: src.slice(last) });
  return out;
}

const STYLE_ID = "cts-json-view-styles";

const STYLE_TEXT = css`
  cts-json-view {
    /* The host is the scroll box. Sizing mirrors cts-json-editor's
       resolveBounds() fallbacks (80px floor / 350px cap); consumer CSS
       on the host class (e.g. .ctsConfigJson, .planConfigJson) overrides
       these with its own min-height/max-height. */
    display: block;
    box-sizing: border-box;
    overflow: auto;
    min-height: 80px;
    max-height: 350px;
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    /* Muted "display surface" canvas + quiet left rail — the same
       read-only affordance the muted Monaco theme gave these views. */
    background: var(--bg-muted);
    box-shadow: inset 2px 0 0 var(--ink-200);
  }
  cts-json-view .oidf-json-view {
    margin: 0;
    padding: 6px 10px;
    font-family: var(--font-mono);
    font-size: var(--fs-13);
    line-height: var(--lh-base);
    color: var(--fg);
    white-space: pre;
    tab-size: 2;
  }
  /* Restrained, on-brand syntax palette: structural keys in strong ink,
     strings/numbers in the system green/blue, literals in brand orange,
     punctuation muted. Kept deliberately small so it reads as highlighted
     without importing an off-brand rainbow. */
  cts-json-view .json-key {
    color: var(--ink-800);
    font-weight: 600;
  }
  cts-json-view .json-string {
    color: var(--status-pass);
  }
  cts-json-view .json-number {
    color: var(--status-running);
  }
  cts-json-view .json-keyword {
    color: var(--orange-700);
  }
`;

/**
 * Append the scoped stylesheet to `<head>` once per page lifetime. The
 * `STYLE_ID` guard means N views on the same page share one rule set.
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

customElements.define("cts-json-view", CtsJsonView);

export {};
