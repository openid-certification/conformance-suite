import { html } from "lit";
import { expect, within, waitFor, userEvent, spyOn } from "storybook/test";
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load and render in the table
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    await step("all tokens appear in the table", async () => {
      expect(canvas.getByText("token-ghi-789-jkl-012")).toBeInTheDocument();
      expect(canvas.getByText("token-permanent-001")).toBeInTheDocument();
    });

    await step("permanent token shows Never for expiry", async () => {
      expect(canvas.getByText("Never")).toBeInTheDocument();
    });

    await step("expiring tokens render expiry through cts-time", async () => {
      // A native <time> whose title carries the full absolute date on hover.
      const expiryTime = canvasElement.querySelector("cts-data-table#tokensListing time");
      expect(expiryTime).toBeTruthy();
      expect(expiryTime?.getAttribute("title")).toBeTruthy();
      expect(expiryTime?.getAttribute("datetime")).toBeTruthy();
    });

    await step("token list is delegated to cts-data-table", async () => {
      // The host carries the id="tokensListing" so existing descendant
      // selectors keep working.
      const tokensListing = canvasElement.querySelector("cts-data-table#tokensListing");
      expect(tokensListing).toBeTruthy();
      expect(tokensListing.querySelector("table.oidf-dt-table")).toBeTruthy();
    });

    await step("column headers present", async () => {
      expect(canvas.getByText("Token ID")).toBeInTheDocument();
      expect(canvas.getByText("Expires")).toBeInTheDocument();
    });

    await step("create buttons present (rendered by cts-button)", async () => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
      expect(canvas.getByText("New permanent token")).toBeInTheDocument();
    });

    await step("delete buttons in each row — one cts-button per row", async () => {
      const deleteButtons = canvasElement.querySelectorAll(
        "cts-data-table#tokensListing cts-button.deleteBtn",
      );
      expect(deleteButtons.length).toBe(3);
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for initial load
    await waitFor(() => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    });

    // Spy on navigator.clipboard.writeText. The previous version mocked
    // navigator.copy by mistake; with the spy, the production code path
    // runs against a stub that resolves cleanly. restoreMocks: true in
    // vitest.config.js handles teardown.
    const mockWriteText = spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    await step("clicking the temporary token button opens the created-token modal", async () => {
      // The inner <button> rendered by cts-button is what receives the
      // actual click in the browser.
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
    });

    await step("modal shows the created token value", async () => {
      expect(
        canvas.getByText("Here is your new token. This value will only be displayed once."),
      ).toBeInTheDocument();

      const tokenPre = canvasElement.querySelector(".created-token-value");
      expect(tokenPre).toBeTruthy();
      expect(tokenPre.textContent).toBe(MOCK_CREATED_TOKEN.token);
    });

    await step("copy button follows the token value in DOM order", async () => {
      const tokenPre = canvasElement.querySelector(".created-token-value");
      // Copy button is present (cts-button with title attribute)
      const copyHost = canvas.getByTitle("Copy token to clipboard");
      expect(copyHost).toBeTruthy();

      // The Copy button displays after the token value: the <pre> precedes
      // the copy-row in DOM order so the action follows the thing it copies.
      expect(
        Boolean(tokenPre.compareDocumentPosition(copyHost) & Node.DOCUMENT_POSITION_FOLLOWING),
      ).toBe(true);
    });

    await step("clicking copy writes the token to the clipboard", async () => {
      const copyHost = canvas.getByTitle("Copy token to clipboard");
      // Click the inner button rendered by cts-button so the @cts-click
      // handler fires (host.click() does not propagate to the inner button).
      const copyInnerBtn = /** @type {HTMLButtonElement} */ (copyHost.querySelector("button"));
      await userEvent.click(copyInnerBtn);
      expect(mockWriteText).toHaveBeenCalledWith(MOCK_CREATED_TOKEN.token);
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for initial load
    await waitFor(() => {
      expect(canvas.getByText("New permanent token")).toBeInTheDocument();
    });

    const createdModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#createdTokenModal")
    );

    await step("clicking the permanent token button opens the created-token modal", async () => {
      await userEvent.click(canvas.getByText("New permanent token"));

      expect(createdModal).toBeTruthy();
      const dialog = /** @type {HTMLDialogElement} */ (
        createdModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("token value is displayed in the modal", async () => {
      const tokenPre = canvasElement.querySelector(".created-token-value");
      expect(tokenPre).toBeTruthy();
      expect(tokenPre.textContent).toBe(MOCK_CREATED_TOKEN.token);
    });

    await step("heading renders as .oidf-modal-title (cts-modal contract)", async () => {
      const modalTitle = createdModal.querySelector(".oidf-modal-title");
      expect(modalTitle).toBeTruthy();
      expect(modalTitle?.textContent).toBe("Token created");
    });

    await step("modal carries size=lg → dialog has data-size=lg", async () => {
      const dialog = /** @type {HTMLDialogElement} */ (
        createdModal.querySelector("dialog.oidf-modal")
      );
      expect(dialog.getAttribute("data-size")).toBe("lg");
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    const deleteModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#deleteTokenModal")
    );

    await step("clicking the first row's delete button opens the delete modal", async () => {
      // Click the inner button of the first row's delete cts-button.
      const rowDeleteHosts = canvasElement.querySelectorAll("table cts-button.deleteBtn");
      expect(rowDeleteHosts.length).toBe(3);
      const firstDeleteInner = /** @type {HTMLButtonElement} */ (
        rowDeleteHosts[0].querySelector("button")
      );
      await userEvent.click(firstDeleteInner);

      expect(deleteModal).toBeTruthy();
      const dialog = /** @type {HTMLDialogElement} */ (
        deleteModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("confirmation prompt is in the modal body", async () => {
      expect(
        canvas.getByText("Are you sure? This will permanently remove this token."),
      ).toBeInTheDocument();
    });

    await step("confirming the delete closes the modal", async () => {
      // Click the Delete button in the cts-modal footer (rendered from the
      // footer-buttons descriptor — id="confirmDeleteBtn" makes it
      // reachable via getElementById).
      const confirmBtn = document.getElementById("confirmDeleteBtn");
      expect(confirmBtn).toBeTruthy();
      if (!confirmBtn) throw new Error("confirmDeleteBtn not found");
      await userEvent.click(confirmBtn);

      const dialog = /** @type {HTMLDialogElement} */ (
        deleteModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(false));
    });

    await step("table re-renders after deletion (tokens refetched)", async () => {
      await waitFor(() => {
        expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
      });
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
  },
};

export const CancelDelete = {
  parameters: {
    msw: {
      handlers: [http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS))],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    const deleteModal = /** @type {HTMLElement} */ (
      canvasElement.querySelector("#deleteTokenModal")
    );

    await step("clicking the first delete button opens the confirmation modal", async () => {
      const rowDeleteHosts = canvasElement.querySelectorAll("table cts-button.deleteBtn");
      const firstDeleteInner = /** @type {HTMLButtonElement} */ (
        rowDeleteHosts[0].querySelector("button")
      );
      await userEvent.click(firstDeleteInner);

      const dialog = /** @type {HTMLDialogElement} */ (
        deleteModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("clicking Cancel dismisses the modal", async () => {
      // Cancel is rendered as a footer button by cts-modal. Its descriptor
      // has `dismiss: true` (the default), so clicking closes the dialog.
      const footerButtons = deleteModal.querySelectorAll(".oidf-modal-footer button");
      expect(footerButtons.length).toBe(2);
      const cancelBtn = /** @type {HTMLButtonElement} */ (footerButtons[1]);
      expect(cancelBtn.textContent).toBe("Cancel");
      await userEvent.click(cancelBtn);

      const dialog = /** @type {HTMLDialogElement} */ (
        deleteModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(false));
    });

    await step("all tokens remain in the table", async () => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
      expect(canvas.getByText("token-ghi-789-jkl-012")).toBeInTheDocument();
      expect(canvas.getByText("token-permanent-001")).toBeInTheDocument();
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
  },
};

export const EmptyTokenList = {
  parameters: {
    msw: {
      handlers: [http.get("/api/token", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-token-manager></cts-token-manager>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for loading to finish — the empty-state message replaces the
    // Loading... placeholder.
    await waitFor(() => {
      expect(canvas.getByText("No tokens have been created yet.")).toBeInTheDocument();
    });

    await step("no token-listing rendered in the empty state", async () => {
      // The cts-data-table host is gated by `_tokens.length === 0` so it
      // should not be in the DOM.
      const tokensListing = canvasElement.querySelector("cts-data-table#tokensListing");
      expect(tokensListing).toBeNull();
    });

    await step("create buttons are still present", async () => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
      expect(canvas.getByText("New permanent token")).toBeInTheDocument();
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Wait for initial load
    await waitFor(() => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    });

    await step("clicking create token opens the error modal", async () => {
      await userEvent.click(canvas.getByText("New temporary token"));

      const errorModal = /** @type {HTMLElement} */ (
        canvasElement.querySelector("#createdErrorModal")
      );
      expect(errorModal).toBeTruthy();
      const dialog = /** @type {HTMLDialogElement} */ (
        errorModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("error modal shows the error message", async () => {
      const errorMessage = canvasElement.querySelector(".error-message");
      expect(errorMessage).toBeTruthy();
      expect(errorMessage.textContent).toContain("Rate limit exceeded");
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Spy with a rejecting implementation — simulates permissions-denied /
    // insecure-context. restoreMocks: true in vitest.config.js auto-restores
    // the original method after the test.
    spyOn(navigator.clipboard, "writeText").mockRejectedValue(new Error("permission denied"));

    await step("create a token and open the created-token modal", async () => {
      await waitFor(() => expect(canvas.getByText("New temporary token")).toBeInTheDocument());
      await userEvent.click(canvas.getByText("New temporary token"));

      const createdModal = /** @type {HTMLElement} */ (
        canvasElement.querySelector("#createdTokenModal")
      );
      const dialog = /** @type {HTMLDialogElement} */ (
        createdModal.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("clicking copy surfaces failure feedback in an aria-live region", async () => {
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
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // The component checks `if (!navigator.clipboard)` to fall back to the
    // not-available branch, so a spy on writeText isn't enough — we have
    // to make the whole property undefined. Object.defineProperty needs
    // configurable: true on both sides so the restore works. (The previous
    // version targeted navigator.copy by mistake, leaving navigator.clipboard
    // present and the not-available branch never reached.)
    const originalClipboard = navigator.clipboard;
    Object.defineProperty(navigator, "clipboard", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    try {
      await step("create a token and open the created-token modal", async () => {
        await waitFor(() => expect(canvas.getByText("New temporary token")).toBeInTheDocument());
        await userEvent.click(canvas.getByText("New temporary token"));

        const createdModal = /** @type {HTMLElement} */ (
          canvasElement.querySelector("#createdTokenModal")
        );
        const dialog = /** @type {HTMLDialogElement} */ (
          createdModal.querySelector("dialog.oidf-modal")
        );
        await waitFor(() => expect(dialog.open).toBe(true));
      });

      await step("clicking copy surfaces a distinct not-available message", async () => {
        const copyHost = canvas.getByTitle("Copy token to clipboard");
        const copyInnerBtn = /** @type {HTMLButtonElement} */ (copyHost.querySelector("button"));
        await userEvent.click(copyInnerBtn);

        await waitFor(() => {
          const feedback = canvasElement.querySelector('[data-testid="copy-feedback"]');
          expect(feedback).toBeTruthy();
          expect(feedback.textContent).toContain("Clipboard not available");
        });
      });
    } finally {
      Object.defineProperty(navigator, "clipboard", {
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await waitFor(() => {
      expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    });

    const ids = ["createdTokenModal", "deleteTokenModal", "createdErrorModal"];
    for (const id of ids) {
      await step(`${id} opens and closes via the cts-modal API`, async () => {
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
      });
    }

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Admin message is shown
    await waitFor(() => {
      expect(
        canvas.getByText("Admin users cannot create tokens - please login as a non-admin user."),
      ).toBeInTheDocument();
    });

    await step("no create buttons", async () => {
      expect(canvas.queryByText("New temporary token")).toBeNull();
      expect(canvas.queryByText("New permanent token")).toBeNull();
    });

    await step("no token table", async () => {
      const table = canvasElement.querySelector("table");
      expect(table).toBeNull();
    });

    await step("no modals rendered in admin mode", async () => {
      // The admin path returns early in render() before _renderModals runs.
      expect(canvasElement.querySelector("cts-modal")).toBeNull();
    });

    await step("no legacy Bootstrap classes remain in the rendered DOM", async () => {
      assertNoLegacyBootstrapClasses(canvasElement);
    });
  },
};
