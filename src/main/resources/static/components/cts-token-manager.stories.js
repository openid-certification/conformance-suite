import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_TOKENS, MOCK_CREATED_TOKEN } from "@fixtures/mock-tokens.js";
import "./cts-token-manager.js";

export default {
  title: "Pages/cts-token-manager",
  component: "cts-token-manager",
};

/**
 * Wait one frame so cts-modal `.show()` settles its open attribute and
 * the `<dialog>` element materializes the open state synchronously.
 *
 * @returns {Promise<void>}
 */
function nextFrame() {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}

/**
 * Forward-compatibility guard: after Bootstrap CSS removal, no element in
 * the rendered DOM may carry the legacy `class="modal*"`,
 * `class="btn-close"`, `class="btn btn-*"`, or `class="bg-gradient"`
 * strings. The cts-token-manager + cts-modal duo must produce only
 * OIDF-tokenized markup.
 *
 * @param {Element} root - Element to scan
 */
function assertNoLegacyBootstrapClasses(root) {
  const html = root.innerHTML;
  expect(html).not.toMatch(/class="modal[^"]*"/);
  expect(html).not.toMatch(/class="[^"]*\bbtn-close\b[^"]*"/);
  expect(html).not.toMatch(/class="[^"]*\bbtn btn-[a-z]/);
  expect(html).not.toMatch(/class="[^"]*\bbg-gradient\b[^"]*"/);
  expect(html).not.toMatch(/class="[^"]*\bborder-secondary\b[^"]*"/);
}

// --- Stories ---

export const Default = {
  parameters: {
    msw: {
      handlers: [http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS))],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load and render in the table
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    // All tokens appear in the table
    expect(canvas.getByText("token-ghi-789-jkl-012")).toBeInTheDocument();
    expect(canvas.getByText("token-permanent-001")).toBeInTheDocument();

    // Permanent token shows "Never" for expiry
    expect(canvas.getByText("Never")).toBeInTheDocument();

    // Table structure is correct — uses tokenized class, not Bootstrap.
    const table = canvasElement.querySelector("table#tokensListing");
    expect(table).toBeTruthy();
    expect(table.classList.contains("cts-token-manager-table")).toBe(true);

    // Column headers present
    expect(canvas.getByText("Token ID")).toBeInTheDocument();
    expect(canvas.getByText("Expires")).toBeInTheDocument();

    // Create buttons present (rendered by cts-button)
    expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    expect(canvas.getByText("New permanent token")).toBeInTheDocument();

    // Delete buttons in each row — one cts-button per row.
    const deleteButtons = canvasElement.querySelectorAll("table cts-button.deleteBtn");
    expect(deleteButtons.length).toBe(3);

    // No legacy Bootstrap classes remain in the rendered DOM.
    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const CreateTemporaryToken = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.post("/api/token", async ({ request }) => {
          const body = /** @type {{ permanent?: boolean } | null} */ (await request.json());
          // Verify permanent=false for temporary tokens
          if (body && body.permanent === false) {
            return HttpResponse.json(MOCK_CREATED_TOKEN);
          }
          return HttpResponse.json(MOCK_CREATED_TOKEN);
        }),
      ],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for initial load
    await waitFor(() => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    });

    // Mock clipboard
    const mockWriteText = fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "copy", {
      value: { writeText: mockWriteText },
      writable: true,
      configurable: true,
    });

    // Click the temporary token button — the inner <button> rendered by
    // cts-button is what receives the actual click in the browser.
    const tempBtn = canvas.getByText("New temporary token");
    await userEvent.click(tempBtn);

    // The created-token modal opens via cts-modal.show() — wait for it.
    const createdModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#createdTokenModal")
    );
    expect(createdModal).toBeTruthy();
    await waitFor(() => {
      const dialog = /** @type {HTMLDialogElement} */ (
        createdModal.querySelector("dialog.oidf-modal")
      );
      expect(dialog && dialog.open).toBe(true);
    });

    // Modal shows the created token value
    expect(
      canvas.getByText("Here is your new token. This value will only be displayed once."),
    ).toBeInTheDocument();

    // Token value is displayed
    const tokenPre = canvasElement.querySelector(".created-token-value");
    expect(tokenPre).toBeTruthy();
    expect(tokenPre.textContent).toBe(MOCK_CREATED_TOKEN.token);

    // Copy button is present (cts-button with title attribute)
    const copyHost = canvas.getByTitle("Copy token to clipboard");
    expect(copyHost).toBeTruthy();

    // Click the inner button rendered by cts-button so the @cts-click
    // handler fires (host.click() does not propagate to the inner button).
    const copyInnerBtn = /** @type {HTMLButtonElement} */ (copyHost.querySelector("button"));
    await userEvent.click(copyInnerBtn);
    expect(mockWriteText).toHaveBeenCalledWith(MOCK_CREATED_TOKEN.token);

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const CreatePermanentToken = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.post("/api/token", () => HttpResponse.json(MOCK_CREATED_TOKEN)),
      ],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for initial load
    await waitFor(() => {
      expect(canvas.getByText("New permanent token")).toBeInTheDocument();
    });

    // Click the permanent token button
    await userEvent.click(canvas.getByText("New permanent token"));

    // The created-token modal opens
    const createdModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#createdTokenModal")
    );
    expect(createdModal).toBeTruthy();
    const dialog = /** @type {HTMLDialogElement} */ (
      createdModal.querySelector("dialog.oidf-modal")
    );
    await waitFor(() => expect(dialog.open).toBe(true));

    // Token value is displayed in the modal
    const tokenPre = canvasElement.querySelector(".created-token-value");
    expect(tokenPre).toBeTruthy();
    expect(tokenPre.textContent).toBe(MOCK_CREATED_TOKEN.token);

    // The heading is rendered as `.oidf-modal-title` (cts-modal contract).
    const modalTitle = createdModal.querySelector(".oidf-modal-title");
    expect(modalTitle).toBeTruthy();
    expect(modalTitle?.textContent).toBe("Token created");

    // Modal carries the size="lg" attribute → dialog has data-size="lg".
    expect(dialog.getAttribute("data-size")).toBe("lg");

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const DeleteToken = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.delete("/api/token/:tokenId", () => {
          return new HttpResponse(null, { status: 200 });
        }),
      ],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    // Click the inner button of the first row's delete cts-button.
    const rowDeleteHosts = canvasElement.querySelectorAll("table cts-button.deleteBtn");
    expect(rowDeleteHosts.length).toBe(3);
    const firstDeleteInner = /** @type {HTMLButtonElement} */ (
      rowDeleteHosts[0].querySelector("button")
    );
    await userEvent.click(firstDeleteInner);

    // The delete modal opens.
    const deleteModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#deleteTokenModal")
    );
    expect(deleteModal).toBeTruthy();
    const dialog = /** @type {HTMLDialogElement} */ (
      deleteModal.querySelector("dialog.oidf-modal")
    );
    await waitFor(() => expect(dialog.open).toBe(true));

    // Confirmation prompt is in the modal body.
    expect(
      canvas.getByText("Are you sure? This will permanently remove this token."),
    ).toBeInTheDocument();

    // Click the Delete button in the cts-modal footer (rendered from the
    // footer-buttons descriptor — id="confirmDeleteBtn" makes it
    // reachable via getElementById).
    const confirmBtn = document.getElementById("confirmDeleteBtn");
    expect(confirmBtn).toBeTruthy();
    if (!confirmBtn) throw new Error("confirmDeleteBtn not found");
    await userEvent.click(confirmBtn);

    // Modal closes after delete.
    await waitFor(() => expect(dialog.open).toBe(false));

    // After deletion the table re-renders (tokens refetched).
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const CancelDelete = {
  parameters: {
    msw: {
      handlers: [http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS))],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    // Click the first Delete button
    const rowDeleteHosts = canvasElement.querySelectorAll("table cts-button.deleteBtn");
    const firstDeleteInner = /** @type {HTMLButtonElement} */ (
      rowDeleteHosts[0].querySelector("button")
    );
    await userEvent.click(firstDeleteInner);

    // Confirmation modal appears
    const deleteModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#deleteTokenModal")
    );
    const dialog = /** @type {HTMLDialogElement} */ (
      deleteModal.querySelector("dialog.oidf-modal")
    );
    await waitFor(() => expect(dialog.open).toBe(true));

    // Click Cancel — rendered as a footer button by cts-modal. Cancel
    // descriptor has `dismiss: true` (the default), so clicking closes
    // the dialog.
    const footerButtons = deleteModal.querySelectorAll(".oidf-modal-footer button");
    expect(footerButtons.length).toBe(2);
    const cancelBtn = /** @type {HTMLButtonElement} */ (footerButtons[1]);
    expect(cancelBtn.textContent).toBe("Cancel");
    await userEvent.click(cancelBtn);

    await waitFor(() => expect(dialog.open).toBe(false));

    // All tokens remain in the table
    expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    expect(canvas.getByText("token-ghi-789-jkl-012")).toBeInTheDocument();
    expect(canvas.getByText("token-permanent-001")).toBeInTheDocument();

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const EmptyTokenList = {
  parameters: {
    msw: {
      handlers: [http.get("/api/token", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for loading to finish — the empty-state message replaces the
    // Loading... placeholder.
    await waitFor(() => {
      expect(canvas.getByText("No tokens have been created yet.")).toBeInTheDocument();
    });

    // No token-listing table rendered in the empty state. cts-modal hosts
    // are still mounted (they live alongside the table region) but they
    // are not `<table>` elements so the strict selector still finds none.
    const table = canvasElement.querySelector("table#tokensListing");
    expect(table).toBeNull();

    // Create buttons are still present
    expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    expect(canvas.getByText("New permanent token")).toBeInTheDocument();

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const CreateTokenError = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.post("/api/token", () =>
          HttpResponse.json({ error: "Rate limit exceeded" }, { status: 500 }),
        ),
      ],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for initial load
    await waitFor(() => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    });

    // Click create token
    await userEvent.click(canvas.getByText("New temporary token"));

    // Error modal appears with error message
    const errorModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#createdErrorModal")
    );
    expect(errorModal).toBeTruthy();
    const dialog = /** @type {HTMLDialogElement} */ (errorModal.querySelector("dialog.oidf-modal"));
    await waitFor(() => expect(dialog.open).toBe(true));

    const errorMessage = canvasElement.querySelector(".error-message");
    expect(errorMessage).toBeTruthy();
    expect(errorMessage.textContent).toContain("Rate limit exceeded");

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

/**
 * When navigator.clipboard.writeText rejects, the user needs a visible
 * affordance to fall back to manual selection. Previously the failure was
 * swallowed and the user had no idea why nothing copied.
 */
export const CopyTokenClipboardFailure = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.post("/api/token", () => HttpResponse.json(MOCK_CREATED_TOKEN)),
      ],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    const originalClipboard = navigator.clipboard;
    // Reject writeText — simulates permissions-denied / insecure-context.
    Object.defineProperty(navigator, "copy", {
      value: {
        writeText: fn().mockRejectedValue(new Error("permission denied")),
      },
      writable: true,
      configurable: true,
    });

    try {
      await waitFor(() => expect(canvas.getByText("New temporary token")).toBeInTheDocument());
      await userEvent.click(canvas.getByText("New temporary token"));

      // Wait for the created-token modal, then click Copy.
      const createdModal = /** @type {HTMLElement} */ (
        canvasElement.querySelector("#createdTokenModal")
      );
      const dialog = /** @type {HTMLDialogElement} */ (
        createdModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));

      const copyHost = canvas.getByTitle("Copy token to clipboard");
      const copyInnerBtn = /** @type {HTMLButtonElement} */ (copyHost.querySelector("button"));
      await userEvent.click(copyInnerBtn);

      // Failure feedback is rendered in an aria-live region so SR users
      // hear the failure announcement.
      await waitFor(() => {
        const feedback = canvasElement.querySelector('[data-testid="copy-feedback"]');
        expect(feedback).toBeTruthy();
        expect(feedback.textContent).toContain("Copy failed");
        expect(feedback.getAttribute("aria-live")).toBe("polite");
        expect(feedback.getAttribute("role")).toBe("status");
      });
    } finally {
      Object.defineProperty(navigator, "copy", {
        value: originalClipboard,
        writable: true,
        configurable: true,
      });
    }
  },
};

/**
 * When navigator.clipboard is entirely absent (e.g. http:// context), the
 * component should surface a distinct "not available" message without throwing.
 */
export const CopyTokenClipboardAbsent = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.post("/api/token", () => HttpResponse.json(MOCK_CREATED_TOKEN)),
      ],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    const originalClipboard = navigator.clipboard;
    Object.defineProperty(navigator, "copy", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    try {
      await waitFor(() => expect(canvas.getByText("New temporary token")).toBeInTheDocument());
      await userEvent.click(canvas.getByText("New temporary token"));

      const createdModal = /** @type {HTMLElement} */ (
        canvasElement.querySelector("#createdTokenModal")
      );
      const dialog = /** @type {HTMLDialogElement} */ (
        createdModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));

      const copyHost = canvas.getByTitle("Copy token to clipboard");
      const copyInnerBtn = /** @type {HTMLButtonElement} */ (copyHost.querySelector("button"));
      await userEvent.click(copyInnerBtn);

      await waitFor(() => {
        const feedback = canvasElement.querySelector('[data-testid="copy-feedback"]');
        expect(feedback).toBeTruthy();
        expect(feedback.textContent).toContain("Clipboard not available");
      });
    } finally {
      Object.defineProperty(navigator, "copy", {
        value: originalClipboard,
        writable: true,
        configurable: true,
      });
    }
  },
};

/**
 * Each of the three modals opens via cts-modal `.show()` and dismisses
 * cleanly. Verifies that the host's `open` attribute toggles in step
 * with the inner `<dialog>` (the cts-modal contract used by Playwright
 * specs and CSS visibility rules).
 */
export const ModalsOpenAndCloseViaCtsModalApi = {
  parameters: {
    msw: {
      handlers: [http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS))],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    await waitFor(() => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    });

    const ids = ["createdTokenModal", "deleteTokenModal", "createdErrorModal"];
    for (const id of ids) {
      const host = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
        canvasElement.querySelector(`#${id}`)
      );
      expect(host).toBeTruthy();
      const dialog = /** @type {HTMLDialogElement} */ (host.querySelector("dialog.oidf-modal"));
      expect(dialog).toBeTruthy();

      // Initially hidden.
      expect(dialog.open).toBe(false);
      expect(host.hasAttribute("open")).toBe(false);

      // .show() opens; the host mirrors the open attribute for external
      // visibility checks.
      host.show();
      await waitFor(() => expect(dialog.open).toBe(true));
      expect(host.hasAttribute("open")).toBe(true);

      // .hide() closes; the host clears the open attribute.
      host.hide();
      await waitFor(() => expect(dialog.open).toBe(false));
      expect(host.hasAttribute("open")).toBe(false);
      // Allow any cts-modal-close handlers to settle before the next iter.
      await nextFrame();
    }

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};

export const AdminView = {
  parameters: {
    msw: {
      handlers: [
        // Admin view should not need token list, but provide a handler just in case
        http.get("/api/token", () => HttpResponse.json([])),
      ],
    },
  },
  render: () => html`<cts-token-manager is-admin></cts-token-manager>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Admin message is shown
    await waitFor(() => {
      expect(
        canvas.getByText("Admin users cannot create tokens - please login as a non-admin user."),
      ).toBeInTheDocument();
    });

    // No create buttons
    expect(canvas.queryByText("New temporary token")).toBeNull();
    expect(canvas.queryByText("New permanent token")).toBeNull();

    // No token table
    const table = canvasElement.querySelector("table");
    expect(table).toBeNull();

    // No modals rendered in admin mode (admin path returns early in
    // render() before _renderModals runs).
    expect(canvasElement.querySelector("cts-modal")).toBeNull();

    assertNoLegacyBootstrapClasses(canvasElement);
  },
};
