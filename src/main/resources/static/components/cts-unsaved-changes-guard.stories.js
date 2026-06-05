import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import "./cts-unsaved-changes-guard.js";

export default {
  title: "Behaviour/cts-unsaved-changes-guard",
  component: "cts-unsaved-changes-guard",
  parameters: { layout: "padded" },
};

/**
 * Strongly-typed accessor used across stories to call the public API.
 * @param {Element | null} el
 * @returns {Element & { dirty: boolean; markClean: () => void; markDirty: () => void }}
 */
function asGuard(el) {
  return /** @type {any} */ (el);
}

// Stories use a fixed id so the internal modal ids resolve deterministically
// (`<id>-modal-leave` / `<id>-modal-stay`). The guard tolerates a missing id
// at runtime — these tests are the only consumers that lean on the
// deterministic suffix.
const GUARD_ID = "story-guard";

export const Pristine = {
  render: () => html`
    <div>
      <form id="story-form">
        <label>Field <input type="text" name="x" /></label>
      </form>
      <cts-unsaved-changes-guard id="${GUARD_ID}" for="story-form"> </cts-unsaved-changes-guard>
    </div>
  `,

  async play({ canvasElement }) {
    const guard = asGuard(canvasElement.querySelector("cts-unsaved-changes-guard"));
    await waitFor(() => expect(guard).toBeTruthy());
    expect(guard.dirty).toBe(false);
    expect(guard.hasAttribute("dirty")).toBe(false);
  },
};

export const DirtyAfterFormEdit = {
  render: () => html`
    <div>
      <form id="story-form">
        <label>Field <input type="text" name="x" data-testid="story-input" /></label>
      </form>
      <cts-unsaved-changes-guard id="${GUARD_ID}" for="story-form"> </cts-unsaved-changes-guard>
    </div>
  `,

  async play({ canvasElement, step }) {
    const guard = asGuard(canvasElement.querySelector("cts-unsaved-changes-guard"));
    const input = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector('input[data-testid="story-input"]')
    );

    await waitFor(() => expect(guard.dirty).toBe(false));

    await step("typing into the form marks the guard dirty", async () => {
      await userEvent.type(input, "a");

      await waitFor(() => {
        expect(guard.dirty).toBe(true);
        expect(guard.hasAttribute("dirty")).toBe(true);
      });
    });
  },
};

export const DirtyAfterConfigChange = {
  render: () => html`
    <div>
      <!-- Stub element standing in for cts-config-form — the guard only cares
           about the cts-config-change event, not the element type. -->
      <div id="story-config-form"></div>
      <cts-unsaved-changes-guard
        id="${GUARD_ID}"
        config-form-id="story-config-form"
      ></cts-unsaved-changes-guard>
    </div>
  `,

  async play({ canvasElement, step }) {
    const guard = asGuard(canvasElement.querySelector("cts-unsaved-changes-guard"));
    const stub = canvasElement.querySelector("#story-config-form");

    await waitFor(() => expect(guard.dirty).toBe(false));

    await step("a cts-config-change event marks the guard dirty", async () => {
      stub.dispatchEvent(
        new CustomEvent("cts-config-change", {
          bubbles: true,
          detail: { config: { foo: "bar" } },
        }),
      );

      await waitFor(() => expect(guard.dirty).toBe(true));
    });
  },
};

export const LinkClickIntercepted = {
  render: () => html`
    <div>
      <form id="story-form"></form>
      <cts-unsaved-changes-guard id="${GUARD_ID}" for="story-form"> </cts-unsaved-changes-guard>
      <a id="story-link" href="${window.location.origin}/plans.html">Plans</a>
    </div>
  `,

  async play({ canvasElement, step }) {
    const guard = asGuard(canvasElement.querySelector("cts-unsaved-changes-guard"));
    const link = /** @type {HTMLAnchorElement} */ (canvasElement.querySelector("#story-link"));

    await step("marking dirty arms the guard", async () => {
      guard.markDirty();
      expect(guard.dirty).toBe(true);
    });

    await step("clicking a link is intercepted and opens the modal", async () => {
      // Intercept the synthetic click so the play test does not navigate the
      // Storybook iframe. The guard's capture-phase handler runs before this
      // listener fires, so any preventDefault it called is observable here.
      let defaultPreventedByGuard = false;
      link.addEventListener(
        "click",
        (event) => {
          defaultPreventedByGuard = event.defaultPrevented;
          event.preventDefault();
        },
        { once: true },
      );

      await userEvent.click(link);

      await waitFor(() => {
        expect(defaultPreventedByGuard).toBe(true);
        const dialog = canvasElement.querySelector("dialog.oidf-modal[open]");
        expect(dialog).toBeTruthy();
      });
    });
  },
};

export const StayKeepsTheUserOnPage = {
  render: () => html`
    <div>
      <form id="story-form"></form>
      <cts-unsaved-changes-guard id="${GUARD_ID}" for="story-form"> </cts-unsaved-changes-guard>
      <a id="story-link" href="${window.location.origin}/plans.html">Plans</a>
    </div>
  `,

  async play({ canvasElement, step }) {
    const guard = asGuard(canvasElement.querySelector("cts-unsaved-changes-guard"));
    const link = /** @type {HTMLAnchorElement} */ (canvasElement.querySelector("#story-link"));

    await step("clicking a link while dirty opens the modal", async () => {
      guard.markDirty();
      link.addEventListener("click", (event) => event.preventDefault(), { once: true });
      await userEvent.click(link);

      await waitFor(() => {
        const dialog = canvasElement.querySelector("dialog.oidf-modal[open]");
        expect(dialog).toBeTruthy();
      });
    });

    await step("clicking Stay closes the modal and keeps the dirty flag", async () => {
      const stayBtn = /** @type {HTMLButtonElement} */ (
        canvasElement.querySelector(`#${GUARD_ID}-modal-stay`)
      );
      expect(stayBtn).toBeTruthy();
      stayBtn.click();

      await waitFor(() => {
        expect(canvasElement.querySelector("dialog.oidf-modal[open]")).toBeFalsy();
      });
      // Stay does not clear the dirty flag.
      expect(guard.dirty).toBe(true);
    });
  },
};

export const MarkCleanSuppressesInterception = {
  render: () => html`
    <div>
      <form id="story-form"></form>
      <cts-unsaved-changes-guard id="${GUARD_ID}" for="story-form"> </cts-unsaved-changes-guard>
      <a id="story-link" href="${window.location.origin}/plans.html">Plans</a>
    </div>
  `,

  async play({ canvasElement, step }) {
    const guard = asGuard(canvasElement.querySelector("cts-unsaved-changes-guard"));
    const link = /** @type {HTMLAnchorElement} */ (canvasElement.querySelector("#story-link"));

    await step("marking clean resets the dirty flag", async () => {
      guard.markDirty();
      guard.markClean();
      expect(guard.dirty).toBe(false);
    });

    await step("clicking a link is not intercepted while clean", async () => {
      let defaultPreventedByGuard = false;
      link.addEventListener(
        "click",
        (event) => {
          defaultPreventedByGuard = event.defaultPrevented;
          event.preventDefault();
        },
        { once: true },
      );

      await userEvent.click(link);

      // The guard must NOT have intercepted — defaultPrevented should be false
      // at the time the test listener runs, and the modal should remain closed.
      expect(defaultPreventedByGuard).toBe(false);
      expect(canvasElement.querySelector("dialog.oidf-modal[open]")).toBeFalsy();
    });
  },
};
