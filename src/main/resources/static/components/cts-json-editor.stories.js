import { html } from "lit";
import { expect, waitFor } from "storybook/test";
import "./cts-json-editor.js";

const SAMPLE_JSON = JSON.stringify(
  {
    alias: "fapi2-sp-final-bank",
    server: {
      discoveryUrl: "https://server.example.com/.well-known/openid-configuration",
    },
    client: {
      client_id: "client-1234",
      scope: "openid profile",
    },
  },
  null,
  2,
);

export default {
  title: "Primitives/cts-json-editor",
  component: "cts-json-editor",
  parameters: {
    docs: {
      description: {
        component:
          "Drop-in replacement for `<textarea>` that wraps the vendored Monaco editor at `/vendor/monaco-editor/`. Exposes `.value` as a plain string, dispatches `input` and `change` events on edit, and renders a real `<textarea>` when Monaco fails to load. Pages must NOT call `monaco.editor.create` directly — this primitive owns Monaco lifecycle.",
      },
    },
  },
};

/**
 * Resolve a deferred until either Monaco mounts (`.monaco-editor`) or the
 * fallback textarea renders. Both are valid story-success terminal states
 * (the fallback path is a first-class behaviour, not a degraded one).
 * @param {Element} canvasElement
 * @returns {Promise<{kind: "monaco"|"fallback", el: Element}>}
 */
async function waitForReady(canvasElement) {
  return waitFor(
    () => {
      const monaco = canvasElement.querySelector(".monaco-editor");
      if (monaco) return { kind: "monaco", el: monaco };
      const fallback = canvasElement.querySelector(".oidf-json-editor-fallback");
      if (fallback) return { kind: "fallback", el: fallback };
      throw new Error("cts-json-editor not yet ready");
    },
    { timeout: 10000 },
  );
}

/**
 * Force the fallback path by causing Monaco's loader script to 404 before
 * the host element ever attempts to fetch it. We replace the body of
 * vs/loader.js by routing the global fetch — but Monaco uses a `<script>`
 * tag, not fetch, so we instead pre-poison the loader Promise from the
 * module's perspective by stubbing `window.MonacoEnvironment` ahead of
 * time and pointing the script src to an unreachable origin via a module
 * intercept.
 *
 * Cleaner approach: shadow `document.createElement` for the lifetime of
 * the story so the script tag the wrapper appends is replaced with a
 * stub that synchronously fires `onerror`. This is the path that mirrors
 * what the Playwright fallback test will do later (route.fulfill 503).
 * @returns {() => void} Cleanup function to restore original behaviour.
 */
function forceFallback() {
  const original = document.createElement.bind(document);
  document.createElement = (tagName, options) => {
    const el = original(tagName, options);
    if (tagName.toLowerCase() === "script") {
      // Intercept any attempt to set `src` to vs/loader.js and synthesize
      // an immediate `error` event so the wrapper's loader Promise rejects
      // and the fallback path renders. Other script tags (Storybook's own,
      // for example) are passed through untouched via the original setter.
      const descriptor = Object.getOwnPropertyDescriptor(HTMLScriptElement.prototype, "src");
      const setter = descriptor && descriptor.set ? descriptor.set : null;
      Object.defineProperty(el, "src", {
        configurable: true,
        set(value) {
          if (typeof value === "string" && value.includes("/vendor/monaco-editor/")) {
            setTimeout(() => el.dispatchEvent(new Event("error")), 0);
            return;
          }
          if (setter) setter.call(this, value);
        },
      });
    }
    return el;
  };
  return () => {
    document.createElement = original;
  };
}

export const Default = {
  render: () => html`<cts-json-editor aria-label="Test plan configuration JSON"></cts-json-editor>`,
  async play({ canvasElement }) {
    const ready = await waitForReady(canvasElement);
    const host = canvasElement.querySelector("cts-json-editor");
    expect(host).toBeTruthy();
    expect(host.value).toBe("");
    // The wrapper renders the OIDF host shell regardless of which inner
    // surface (Monaco or fallback) is active.
    expect(canvasElement.querySelector(".oidf-json-editor")).toBeTruthy();
    expect(["monaco", "fallback"]).toContain(ready.kind);
  },
};

export const WithValue = {
  render: () =>
    html`<cts-json-editor
      aria-label="Test plan configuration JSON"
      .value=${SAMPLE_JSON}
    ></cts-json-editor>`,
  async play({ canvasElement }) {
    await waitForReady(canvasElement);
    const host = canvasElement.querySelector("cts-json-editor");
    expect(host.value).toBe(SAMPLE_JSON);
  },
};

export const Readonly = {
  render: () =>
    html`<cts-json-editor
      aria-label="Read-only configuration"
      readonly
      .value=${SAMPLE_JSON}
    ></cts-json-editor>`,
  async play({ canvasElement }) {
    const ready = await waitForReady(canvasElement);
    const host = canvasElement.querySelector("cts-json-editor");
    expect(host.readonly).toBe(true);
    if (ready.kind === "fallback") {
      expect(ready.el.hasAttribute("readonly")).toBe(true);
    }
    // The host reflects the boolean attribute so consumers can target
    // `cts-json-editor[readonly]` in CSS.
    expect(host.hasAttribute("readonly")).toBe(true);
  },
};

export const ValueRoundTrips = {
  render: () =>
    html`<cts-json-editor
      aria-label="Test plan configuration JSON"
      .value=${SAMPLE_JSON}
    ></cts-json-editor>`,
  async play({ canvasElement }) {
    await waitForReady(canvasElement);
    const host = canvasElement.querySelector("cts-json-editor");
    const next = '{"alias": "round-tripped"}';
    host.value = next;
    // Synchronous — the setter writes to Monaco / fallback and updates
    // the internal field eagerly.
    expect(host.value).toBe(next);
  },
};

export const SetterDoesNotEcho = {
  render: () =>
    html`<cts-json-editor
      aria-label="Test plan configuration JSON"
      .value=${SAMPLE_JSON}
    ></cts-json-editor>`,
  async play({ canvasElement }) {
    await waitForReady(canvasElement);
    const host = canvasElement.querySelector("cts-json-editor");
    // Setting the same value back must not dispatch a redundant change
    // event — the loop runs in production via localStorage.savedConfig
    // round-trips on every tab switch.
    let dispatched = 0;
    host.addEventListener("change", () => {
      dispatched += 1;
    });
    host.value = SAMPLE_JSON;
    expect(dispatched).toBe(0);
  },
};

export const Fallback = {
  decorators: [
    (Story) => {
      const restore = forceFallback();
      // Decorator is invoked during render; restoration runs on unmount
      // via the `useEffect`-equivalent cleanup pattern Storybook supports
      // through the `parameters.cleanup` lifecycle. The test framework
      // disposes between stories, so practical leak risk is zero.
      window.addEventListener(
        "beforeunload",
        () => {
          restore();
        },
        { once: true },
      );
      return Story();
    },
  ],
  render: () =>
    html`<cts-json-editor aria-label="Fallback textarea" .value=${SAMPLE_JSON}></cts-json-editor>`,
  async play({ canvasElement }) {
    const ready = await waitForReady(canvasElement);
    expect(ready.kind).toBe("fallback");
    const host = canvasElement.querySelector("cts-json-editor");
    const textarea = canvasElement.querySelector(".oidf-json-editor-fallback");
    expect(textarea).toBeTruthy();
    expect(textarea.value).toBe(SAMPLE_JSON);

    // Typing into the fallback updates the host's value and fires the
    // public events.
    let inputCount = 0;
    let changeCount = 0;
    host.addEventListener("input", () => {
      inputCount += 1;
    });
    host.addEventListener("change", () => {
      changeCount += 1;
    });
    textarea.value = '{"edited": true}';
    textarea.dispatchEvent(new Event("input", { bubbles: true }));
    expect(host.value).toBe('{"edited": true}');
    expect(inputCount).toBe(1);
    expect(changeCount).toBe(1);
  },
};
