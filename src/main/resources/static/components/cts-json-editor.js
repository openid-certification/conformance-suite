import { LitElement, html, nothing } from "lit";

/**
 * Same-origin path to the vendored Monaco AMD distribution. Must end at
 * `vs/` because Monaco's loader uses this as the base path for every
 * dynamic chunk (the editor bundle, language modes, web workers, NLS
 * tables). Changing this constant requires moving the vendored tree —
 * `frontend/scripts/update-vendor-monaco.sh` is the authority.
 * @type {string}
 */
const MONACO_VS_PATH = "/vendor/monaco-editor/vs";

/**
 * Time budget for Monaco's first boot. Beyond this, the wrapper assumes
 * the network or CSP is blocking the loader and falls back to a plain
 * `<textarea>` so the page never hangs on a stuck script tag.
 * @type {number}
 */
const MONACO_LOAD_TIMEOUT_MS = 8000;

/** Minimum editor height in pixels (≈ 4 lines). */
const EDITOR_MIN_HEIGHT_PX = 80;
/** Maximum editor height before the editor scrolls internally. */
const EDITOR_MAX_HEIGHT_PX = 350;

/**
 * Singleton Promise resolving to `window.monaco` once Monaco's AMD bundle
 * has booted. Multiple `<cts-json-editor>` instances on the same page
 * share this Promise — the AMD bundle is fetched once per page lifetime
 * regardless of how many editors render.
 * @type {Promise<typeof window.monaco>|null}
 */
let monacoLoader = null;

/**
 * Inject the Monaco AMD loader script and resolve once
 * `vs/editor/editor.main` has registered. Memoised: subsequent callers
 * receive the same Promise, so no duplicate network requests fire.
 * Rejects on either the loader-script onerror, the editor.main require
 * failure, or the global timeout — any of which falls the wrapper back
 * to its plain-textarea path.
 * @returns {Promise<typeof window.monaco>} Resolves to the live Monaco
 *   namespace once the AMD bundle has registered `vs/editor/editor.main`.
 */
function loadMonaco() {
  if (monacoLoader) return monacoLoader;

  monacoLoader = new Promise((resolve, reject) => {
    if (typeof window === "undefined") {
      reject(new Error("Monaco requires a browser environment"));
      return;
    }
    if (window.monaco?.editor) {
      resolve(window.monaco);
      return;
    }

    const timer = window.setTimeout(() => {
      reject(new Error(`Monaco load timed out after ${MONACO_LOAD_TIMEOUT_MS}ms`));
    }, MONACO_LOAD_TIMEOUT_MS);

    // Monaco's web workers must be served from the same origin as the
    // page or the browser refuses to spawn them. Pinning the URL to our
    // vendored path means we never depend on a Blob shim and we stay
    // CSP-clean (worker-src 'self' is sufficient).
    window.MonacoEnvironment = {
      getWorkerUrl: () => `${MONACO_VS_PATH}/base/worker/workerMain.js`,
    };

    const script = document.createElement("script");
    script.src = `${MONACO_VS_PATH}/loader.js`;
    script.async = true;
    script.onerror = () => {
      window.clearTimeout(timer);
      reject(new Error(`Failed to load ${script.src}`));
    };
    script.onload = () => {
      const amdRequire = window.require;
      if (!amdRequire || typeof amdRequire.config !== "function") {
        window.clearTimeout(timer);
        reject(new Error("AMD require not registered after loader.js executed"));
        return;
      }
      amdRequire.config({ paths: { vs: MONACO_VS_PATH } });
      amdRequire(
        ["vs/editor/editor.main"],
        () => {
          window.clearTimeout(timer);
          if (window.monaco?.editor) {
            resolve(window.monaco);
          } else {
            reject(new Error("vs/editor/editor.main resolved without window.monaco"));
          }
        },
        (err) => {
          window.clearTimeout(timer);
          reject(err instanceof Error ? err : new Error(String(err)));
        },
      );
    };

    document.head.appendChild(script);
  });

  // Reset the loader on rejection so a later instance can retry. Without
  // this a single early failure (e.g. a flaky network during bootstrap)
  // would permanently stick every editor on the page in fallback mode.
  monacoLoader.catch(() => {
    monacoLoader = null;
  });

  return monacoLoader;
}

/**
 * Define the OIDF light theme inside Monaco. Idempotent — Monaco silently
 * re-registers the theme on each call. The colour values resolve from the
 * design-system tokens already on `:root` so the editor visually matches
 * the surrounding form fields without a separate palette to maintain.
 * @param {any} monaco - Live Monaco namespace returned
 *   by `loadMonaco()`; the function calls `monaco.editor.defineTheme`.
 */
function defineOidfTheme(monaco) {
  const cs = window.getComputedStyle(document.documentElement);
  const bg = cs.getPropertyValue("--bg-elev").trim() || "#ffffff";
  // --bg-muted is the design-system token for sunken/display surfaces
  // (alerts, drawer body backgrounds). Read-only editors paint with it
  // so they visually rhyme with surrounding read-only chrome rather
  // than mimicking an editable input.
  const bgMuted = cs.getPropertyValue("--bg-muted").trim() || "#F8F7F5";
  monaco.editor.defineTheme("oidf-light", {
    base: "vs",
    inherit: true,
    rules: [],
    colors: {
      "editor.background": bg,
    },
  });
  monaco.editor.defineTheme("oidf-light-readonly", {
    base: "vs",
    inherit: true,
    rules: [],
    colors: {
      "editor.background": bgMuted,
    },
  });
}

const STYLE_ID = "cts-json-editor-styles";

const STYLE_TEXT = `
/* Custom elements default to display: inline, which makes the host
   collapse to its content's size and prevents min-height from working as
   page-level callers expect. Defaulting the host to display: block lets
   pages omit "display: block" from inline styles; per-page min-height
   (when needed) remains the only inline configuration. */
cts-json-editor {
  display: block;
}
.oidf-json-editor {
  display: block;
  width: 100%;
  border: 1px solid var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  font-family: var(--font-mono);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  overflow: hidden;
  position: relative;
  box-sizing: border-box;
  padding: 6px;
}
/* Focus ring is meaningful only on editable editors. Clicking a
   read-only surface does nothing, so painting the orange ring there
   would mislead users about the affordance. */
cts-json-editor:not([readonly]) .oidf-json-editor:focus-within {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
/* Read-only host paints with the same muted canvas as the Monaco
   theme so the affordance survives the Monaco-pre-mount paint window
   and the fallback textarea path (Monaco failed to load). The left
   inset rail is a quiet visual anchor that signals "display surface"
   without competing with the aria-label or surrounding chrome. */
cts-json-editor[readonly] .oidf-json-editor {
  background: var(--bg-muted);
  box-shadow: inset 2px 0 0 var(--ink-200);
}
.oidf-json-editor-host {
  width: 100%;
  min-height: 80px;
}
.oidf-json-editor-fallback {
  display: block;
  width: 100%;
  min-height: inherit;
  padding: var(--space-3);
  border: 0;
  background: transparent;
  color: var(--fg);
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  resize: vertical;
  text-indent: 0;
  box-sizing: border-box;
}
cts-json-editor[readonly] .oidf-json-editor-fallback {
  background: var(--bg-muted);
}
.oidf-json-editor-fallback:focus {
  outline: none;
}
`;

/**
 * Append the scoped stylesheet to `<head>` once per page lifetime. The
 * `STYLE_ID` guard means N editors on the same page share one rule set
 * regardless of mount order.
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * JSON editor wrapping the vendored Monaco distribution. Drop-in for a
 * `<textarea>`: exposes `.value` as a plain string, dispatches `input`
 * and `change` events on edit, and falls back to a real `<textarea>` if
 * Monaco fails to boot (network error, CSP, upstream parse failure).
 *
 * Light DOM. The editor's chrome is rendered into a placeholder `<div>`
 * inside the host; the host itself carries the focus ring so consumers
 * can target it with `cts-json-editor:focus-within` and similar.
 *
 * Pages should NEVER call `monaco.editor.create(...)` directly — this
 * primitive is the only supported entry point. See
 * `src/main/resources/static/vendor/monaco-editor/README.md`.
 * @property {string} value - Current editor text. Setting `el.value`
 *   updates Monaco synchronously when ready; round-trips through the
 *   fallback textarea when not. Mirrors `<textarea>.value`.
 * @property {string} placeholder - Hint text shown when the editor is
 *   empty. Rendered by the fallback textarea's native placeholder when
 *   Monaco is unavailable; Monaco does not render placeholder text in
 *   the empty editor (the visual hint moves to surrounding labels).
 * @property {boolean} readonly - When true, the editor rejects edits and
 *   paints with the muted "display surface" canvas (`oidf-light-readonly`
 *   theme + `--bg-muted` host background) so the surface itself signals
 *   "display only" rather than mimicking an editable input. Reflected to
 *   Monaco's `readOnly` / `domReadOnly` options and the fallback
 *   textarea's `readonly` attribute. The `[readonly]` attribute on the
 *   host is what consumer CSS targets (the host suppresses the orange
 *   focus ring in this state — clicking a read-only surface is not a
 *   meaningful interaction).
 * @property {string} language - Monaco language id. Defaults to "json".
 *   Changing it after first render rebuilds the model language.
 * @fires input - On every editor edit; bubbles, mirrors `<textarea>`.
 * @fires change - On every editor edit; bubbles, mirrors `<textarea>`.
 *   Both `input` and `change` are dispatched together so listeners on
 *   either name keep working when migrating from a `<textarea>`.
 *
 * Consumers awaiting an interactive editor (Storybook plays, Playwright
 * specs, downstream Lit elements that need to focus on mount) should use
 * `await el.whenReady()` rather than polling for `.monaco-editor` or
 * `.oidf-json-editor-fallback`. The Promise resolves with the same
 * `{kind, el}` shape regardless of which surface mounted, so call sites
 * stay agnostic to whether Monaco booted or the fallback path took over.
 */
class CtsJsonEditor extends LitElement {
  static properties = {
    // `noAccessor: true` lets us keep the manual get/set value pair below.
    // Without it Lit's finalize() would Object.defineProperty over our
    // accessors at prototype level, silently disabling the setter's
    // dispatch-suppression and Monaco sync logic. Lit's reactive update
    // pipeline still fires via the explicit requestUpdate("value", prev)
    // calls inside the setter.
    value: { type: String, noAccessor: true },
    placeholder: { type: String },
    readonly: { type: Boolean, reflect: true },
    language: { type: String },
    _status: { state: true },
  };

  constructor() {
    super();
    /** @type {string} */
    this._value = "";
    /** @type {string} */
    this.placeholder = "";
    /** @type {boolean} */
    this.readonly = false;
    /** @type {string} */
    this.language = "json";
    /** @type {"pending"|"ready"|"fallback"} */
    this._status = "pending";

    /** @type {object|null} Monaco editor instance once ready. */
    this._editor = null;
    /** @type {object|null} Monaco model. */
    this._model = null;
    /** @type {boolean} Internal guard against echo dispatch when our setter writes back to Monaco. */
    this._suppressDispatch = false;

    // Public readiness Promise. Created eagerly in the constructor so that
    // a consumer calling `await el.whenReady()` synchronously after `new`
    // (or immediately after declarative render) never races with
    // `connectedCallback()`. Resolved exactly once — either by the Monaco
    // mount path or the fallback render path inside `_bootMonaco()`.
    /** @type {(value: {kind: "monaco"|"fallback", el: Element}) => void} */
    this._readyResolve = () => {};
    /** @type {Promise<{kind: "monaco"|"fallback", el: Element}>} */
    this._readyPromise = new Promise((resolve) => {
      this._readyResolve = resolve;
    });
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._bootMonaco();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._editor) {
      try {
        this._editor.dispose();
      } catch {
        // Monaco occasionally throws on dispose during page unload; we
        // are tearing down anyway so swallowing is safe.
      }
      this._editor = null;
    }
    if (this._model) {
      try {
        this._model.dispose();
      } catch {
        // Tearing down — swallow per the editor.dispose() rationale above.
      }
      this._model = null;
    }
  }

  /**
   * Kick off Monaco load and instantiate the editor when ready. Stays
   * a no-op if the host is no longer connected by the time the loader
   * resolves — avoids ghost editors on detached hosts (Storybook
   * re-renders, modal close-then-reopen, etc.).
   */
  async _bootMonaco() {
    let monaco;
    try {
      monaco = await loadMonaco();
    } catch (err) {
      // Surface once per page lifetime so devs see the cause in the
      // console without spamming production users.
      if (typeof console !== "undefined" && console.warn) {
        console.warn("cts-json-editor: Monaco failed to load, falling back to textarea.", err);
      }
      this._status = "fallback";
      // Render() commits the fallback DOM on the next Lit cycle; await
      // updateComplete so whenReady() callers get a non-null `el`.
      await this.updateComplete;
      const fallbackEl = this.querySelector(".oidf-json-editor-fallback");
      if (fallbackEl) this._readyResolve({ kind: "fallback", el: fallbackEl });
      return;
    }
    if (!this.isConnected) return;

    defineOidfTheme(monaco);
    this._status = "ready";
    // Wait for Lit to render the host placeholder before mounting Monaco.
    await this.updateComplete;

    const host = /** @type {HTMLElement | null} */ (this.querySelector(".oidf-json-editor-host"));
    if (!host) return;

    this._model = monaco.editor.createModel(this._value || "", this.language);
    this._editor = monaco.editor.create(host, {
      model: this._model,
      theme: this.readonly ? "oidf-light-readonly" : "oidf-light",
      automaticLayout: true,
      readOnly: this.readonly,
      domReadOnly: this.readonly,
      cursorStyle: this.readonly ? "line-thin" : "line",
      minimap: { enabled: false },
      guides: { indentation: false },
      // Monaco renders an "overview ruler" strip on the right edge that
      // proxies the minimap (error markers, cursor positions). For a
      // form-field JSON editor this is decorative chrome we don't need
      // — collapse the lanes and border so the strip disappears flush.
      overviewRulerLanes: 0,
      overviewRulerBorder: false,
      hideCursorInOverviewRuler: true,
      lineNumbers: "off",
      lineDecorationsWidth: 0,
      glyphMargin: false,
      folding: false,
      scrollBeyondLastLine: false,
      fontFamily: "var(--font-mono)",
      fontSize: 13,
      tabSize: 2,
      formatOnPaste: false,
      wordWrap: "off",
    });

    const updateHeight = () => {
      const contentHeight = Math.max(
        EDITOR_MIN_HEIGHT_PX,
        Math.min(EDITOR_MAX_HEIGHT_PX, this._editor.getContentHeight()),
      );
      host.style.height = `${contentHeight}px`;
    };
    this._editor.onDidContentSizeChange(updateHeight);
    updateHeight();

    this._editor.onDidChangeModelContent(() => {
      if (this._suppressDispatch) return;
      const next = this._editor.getValue();
      if (next === this._value) return;
      this._value = next;
      this._dispatchChange();
      this.requestUpdate("value");
    });

    // monaco.editor.create attaches `.monaco-editor` synchronously into the
    // host container, so a query immediately after the call resolves the
    // node consumers want to interact with.
    const monacoNode = host.querySelector(".monaco-editor");
    if (monacoNode) this._readyResolve({ kind: "monaco", el: monacoNode });
  }

  /**
   * Resolves once the editor is interactive — either Monaco has mounted
   * (`kind: "monaco"`) or the fallback `<textarea>` has rendered
   * (`kind: "fallback"`). The Promise is created in the constructor and
   * resolves exactly once per instance, so callers can safely await it
   * any number of times.
   *
   * Storybook plays, Playwright specs, and downstream Lit elements that
   * need to focus / set selection / read `.value` after mount should call
   * this instead of polling for the inner DOM, since the polling pattern
   * is brittle to render-timing changes inside the wrapper.
   * @returns {Promise<{kind: "monaco"|"fallback", el: Element}>} Resolves
   *   with the inner surface node and a discriminator describing which
   *   path mounted. Both kinds satisfy the `.value` contract via the host.
   */
  whenReady() {
    return this._readyPromise;
  }

  /**
   * Reflect the public `value` property setter onto Monaco. Lit calls
   * this whenever a consumer assigns `el.value = "…"`. The
   * `_suppressDispatch` flag guards against an infinite loop where
   * setValue → onDidChangeModelContent → consumer-handler → setValue.
   * @param {string} next - Replacement value. `null`/`undefined` are
   *   coerced to the empty string, matching `<textarea>` semantics.
   */
  set value(next) {
    const prev = this._value;
    const normalised = next == null ? "" : String(next);
    if (normalised === prev) return;
    this._value = normalised;
    if (this._editor && this._editor.getValue() !== normalised) {
      this._suppressDispatch = true;
      try {
        this._editor.setValue(normalised);
      } finally {
        this._suppressDispatch = false;
      }
    }
    this.requestUpdate("value", prev);
  }

  /**
   * @returns {string} Current editor text. Empty string when the
   *   component has never been assigned a value.
   */
  get value() {
    return this._value;
  }

  updated(changed) {
    if (changed.has("readonly") && this._editor) {
      this._editor.updateOptions({
        readOnly: this.readonly,
        domReadOnly: this.readonly,
        cursorStyle: this.readonly ? "line-thin" : "line",
        theme: this.readonly ? "oidf-light-readonly" : "oidf-light",
      });
    }
    if (changed.has("language") && this._model && window.monaco) {
      window.monaco.editor.setModelLanguage(this._model, this.language);
    }
  }

  /**
   * Dispatch `input` and `change` together so consumers migrating from a
   * `<textarea>` keep working regardless of which event they listened on.
   */
  _dispatchChange() {
    this.dispatchEvent(new Event("input", { bubbles: true }));
    this.dispatchEvent(new Event("change", { bubbles: true }));
  }

  _onFallbackInput(e) {
    const next = e.target.value;
    if (next === this._value) return;
    this._value = next;
    this._dispatchChange();
    this.requestUpdate("value");
  }

  render() {
    if (this._status === "fallback") {
      return html`<div class="oidf-json-editor">
        <textarea
          class="oidf-json-editor-fallback"
          .value=${this._value}
          .placeholder=${this.placeholder}
          ?readonly=${this.readonly}
          spellcheck="false"
          autocomplete="off"
          autocorrect="off"
          autocapitalize="off"
          @input=${this._onFallbackInput}
        ></textarea>
      </div>`;
    }
    return html`<div class="oidf-json-editor">
      <div class="oidf-json-editor-host"></div>
      ${this._status === "pending"
        ? html`<span hidden data-cts-json-editor-status="pending"></span>`
        : nothing}
    </div>`;
  }
}

customElements.define("cts-json-editor", CtsJsonEditor);

export {};
