import { LitElement, html } from "lit";
import "./cts-modal.js";

/**
 * `<cts-unsaved-changes-guard>` watches a long-form editing surface and
 * prompts the user before they navigate away with uncommitted edits.
 *
 * The element tracks "dirty" state from two sources:
 *
 * - A `<cts-config-form>` referenced by `config-form-id`. Every
 *   `cts-config-change` it dispatches flips `dirty` to `true`.
 * - A plain `<form>` referenced by `for`. Any bubbling `input` or `change`
 *   event on that form flips `dirty` to `true`.
 *
 * Programmatic state changes (loading a saved configuration, applying a
 * shared URL, submitting the form) must call `markClean()` so the guard
 * does not warn about navigation the user actually intended.
 *
 * Two warning paths are installed at `connectedCallback`:
 *
 * 1. **Browser-level navigation** — `window.beforeunload`. Uses the modern
 *    `event.preventDefault()` + `event.returnValue = ""` pair so Chrome 119+,
 *    Safari, and Firefox all surface the OS-supplied prompt. No-op when
 *    `dirty` is `false`.
 * 2. **In-app link clicks** — capture-phase `document.click`. Same-origin
 *    `<a href>` clicks that would navigate away (no modifiers, no
 *    `target="_blank"`, no `download`, not a pure hash on the current page)
 *    are intercepted and routed through the hosted `<cts-modal>` so the
 *    user gets a styled "Stay / Leave" prompt instead of the generic OS
 *    warning. ESC and backdrop click on the modal are treated as Stay.
 *
 * Both listeners are registered with a shared `AbortSignal` so
 * `disconnectedCallback` can revoke them in a single `abort()` call. This
 * mirrors the cleanup pattern recommended by the modern DOM event spec and
 * already used in other CTS components.
 *
 * Light DOM. The element renders no chrome other than its hosted modal,
 * so there is no scoped stylesheet to inject.
 * @property {string} configFormId - DOM id of the `<cts-config-form>` to
 *   watch for `cts-config-change`. Reflects the `config-form-id` attribute.
 *   Optional.
 * @property {string} for - DOM id of a plain `<form>` to watch for
 *   bubbling `input` / `change` events. Reflects the `for` attribute.
 *   Optional.
 * @property {boolean} dirty - Reactive flag, `true` when the watched
 *   surfaces have been edited since the last `markClean()`. Reflects the
 *   `dirty` attribute so test runners can assert on `[dirty]`.
 * @property {string} heading - Modal heading text. Default
 *   "You have unsaved changes".
 * @property {string} message - Modal body copy. Default explains the
 *   consequence of leaving.
 * @property {string} confirmLabel - Label for the destructive "leave"
 *   button. Default "Leave page".
 * @property {string} cancelLabel - Label for the safe "stay" button.
 *   Default "Stay on page".
 * @fires cts-unsaved-changes-leave - Dispatched on the host when the user
 *   confirms they want to leave, just before navigation. `detail.href`
 *   carries the pending URL. Bubbles.
 * @fires cts-unsaved-changes-stay - Dispatched on the host when the user
 *   dismisses the dialog (Stay button, ESC, backdrop click). Bubbles.
 */
class CtsUnsavedChangesGuard extends LitElement {
  static properties = {
    configFormId: { type: String, attribute: "config-form-id" },
    for: { type: String },
    dirty: { type: Boolean, reflect: true },
    heading: { type: String },
    message: { type: String },
    confirmLabel: { type: String, attribute: "confirm-label" },
    cancelLabel: { type: String, attribute: "cancel-label" },
  };

  constructor() {
    super();
    this.configFormId = "";
    this.for = "";
    this.dirty = false;
    this.heading = "You have unsaved changes";
    this.message =
      "If you leave this page now, the edits you have made to the test plan configuration will be lost.";
    this.confirmLabel = "Leave page";
    this.cancelLabel = "Stay on page";
    this._pendingHref = null;
    this._ac = null;
  }

  /**
   * Render the component into its host element rather than a shadow root.
   * @returns {this} the host element used as the render root
   */
  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._ac = new AbortController();
    const { signal } = this._ac;

    window.addEventListener("beforeunload", this._onBeforeUnload, { signal });
    document.addEventListener("click", this._onDocumentClick, {
      capture: true,
      signal,
    });

    // Defer subject-element wiring to the next microtask so consumers can
    // place this element in document order before the elements it watches
    // and still have the listeners attach correctly.
    queueMicrotask(() => this._attachSubjectListeners(signal));
  }

  disconnectedCallback() {
    if (this._ac) {
      this._ac.abort();
      this._ac = null;
    }
    super.disconnectedCallback();
  }

  /**
   * Resolve and bind listeners to the watched form / config-form. Both
   * references are optional; missing targets are silently skipped so the
   * guard degrades gracefully when host markup is incomplete.
   * @param {AbortSignal} signal - cleanup signal shared across all listeners
   */
  _attachSubjectListeners(signal) {
    if (signal.aborted) return;

    if (this.configFormId) {
      const configForm = document.getElementById(this.configFormId);
      if (configForm) {
        configForm.addEventListener("cts-config-change", this._onSubjectEdit, {
          signal,
        });
      }
    }

    if (this.for) {
      const form = document.getElementById(this.for);
      if (form) {
        form.addEventListener("input", this._onSubjectEdit, { signal });
        form.addEventListener("change", this._onSubjectEdit, { signal });
      }
    }
  }

  _onSubjectEdit = () => {
    if (!this.dirty) this.dirty = true;
  };

  /**
   * Modern `beforeunload` handler. Calling `event.preventDefault()` is the
   * canonical trigger per the current HTML spec (Chrome 119+ requires it);
   * `returnValue = ""` keeps Safari and older Chromium/Firefox happy. The
   * string content is ignored by every modern browser — the OS supplies
   * the message.
   * @param {BeforeUnloadEvent} event - the browser unload event
   */
  _onBeforeUnload = (event) => {
    if (!this.dirty) return;
    event.preventDefault();
    // Spec-deprecated but Safari and older Chromium still trigger the prompt
    // from this property, not from preventDefault() alone. The cast hides
    // the editor strikethrough — the assignment itself is intentional.
    /** @type {{ returnValue: string }} */ (event).returnValue = "";
  };

  /**
   * Capture-phase document click handler. Resolves the candidate anchor
   * and bails out under any condition where the user clearly did not mean
   * to navigate away (modifier keys, new-tab targets, downloads, hash-only
   * links on the current page, cross-origin links, right-clicks). When all
   * checks pass and the form is dirty, the handler intercepts the click
   * and opens the modal.
   * @param {MouseEvent} event - the click event being evaluated for interception
   */
  _onDocumentClick = (event) => {
    if (event.defaultPrevented) return;
    if (event.button !== 0) return;
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    if (!this.dirty) return;

    // event.composedPath() crosses shadow boundaries; closest() does not.
    // Walk the path looking for an <a href>.
    const path = typeof event.composedPath === "function" ? event.composedPath() : [];
    /** @type {HTMLAnchorElement | null} */
    let anchor = null;
    for (const node of path) {
      if (node instanceof HTMLAnchorElement && node.hasAttribute("href")) {
        anchor = node;
        break;
      }
    }
    if (!anchor) return;

    if (anchor.target && anchor.target !== "_self") return;
    if (anchor.hasAttribute("download")) return;
    if (anchor.origin !== window.location.origin) return;
    if (anchor.pathname === window.location.pathname && anchor.search === window.location.search) {
      // Pure hash navigation on the same document — no data loss.
      return;
    }

    event.preventDefault();
    this._pendingHref = anchor.href;
    const modal = /** @type {(Element & { show?: () => void }) | null} */ (
      this.renderRoot.querySelector("cts-modal")
    );
    if (modal && typeof modal.show === "function") {
      modal.show();
    }
  };

  /** Public API: clear the dirty flag. Idempotent. */
  markClean() {
    if (this.dirty) this.dirty = false;
  }

  /** Public API: force the dirty flag on. Useful for tests and edge cases. */
  markDirty() {
    if (!this.dirty) this.dirty = true;
  }

  _onLeaveClick = () => {
    const href = this._pendingHref;
    this._pendingHref = null;
    this.markClean();
    this.dispatchEvent(
      new CustomEvent("cts-unsaved-changes-leave", {
        detail: { href },
        bubbles: true,
      }),
    );
    if (href) {
      window.location.assign(href);
    }
  };

  _onStayClick = () => {
    this._pendingHref = null;
    this.dispatchEvent(new CustomEvent("cts-unsaved-changes-stay", { bubbles: true }));
  };

  render() {
    const modalId = this.id ? `${this.id}-modal` : "cts-unsaved-guard-modal";
    const stayId = `${modalId}-stay`;
    const leaveId = `${modalId}-leave`;
    const footerButtons = JSON.stringify([
      {
        label: this.cancelLabel,
        class: "btn-secondary",
        dismiss: true,
        id: stayId,
      },
      {
        label: this.confirmLabel,
        class: "btn-primary",
        icon: "log-out",
        dismiss: true,
        id: leaveId,
      },
    ]);

    return html`
      <cts-modal
        id="${modalId}"
        heading="${this.heading}"
        size="sm"
        footer-buttons="${footerButtons}"
        @click=${this._onModalButtonClick}
      >
        <p>${this.message}</p>
      </cts-modal>
    `;
  }

  /**
   * Delegated click handler on the modal subtree. cts-modal moves footer
   * buttons into a generated <dialog>, so the buttons exist as descendants
   * of the modal host. Using one delegated listener keeps the guard
   * decoupled from cts-modal's internal DOM shape.
   * @param {MouseEvent} event - delegated click within the modal subtree
   */
  _onModalButtonClick = (event) => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const btn = target.closest("button");
    if (!btn) return;
    const modalId = this.id ? `${this.id}-modal` : "cts-unsaved-guard-modal";
    if (btn.id === `${modalId}-leave`) {
      this._onLeaveClick();
    } else if (btn.id === `${modalId}-stay`) {
      this._onStayClick();
    }
    // Header close button and footer Close fall through with no action —
    // dialog closure is handled by cts-modal; treat as "stay".
  };
}

customElements.define("cts-unsaved-changes-guard", CtsUnsavedChangesGuard);

export {};
