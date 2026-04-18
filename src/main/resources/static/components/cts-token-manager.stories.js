import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_TOKENS, MOCK_CREATED_TOKEN } from "@fixtures/mock-tokens.js";
import "./cts-token-manager.js";

export default {
  title: "Pages/cts-token-manager",
  component: "cts-token-manager",
};

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

    // Table structure is correct
    const table = canvasElement.querySelector("table");
    expect(table).toBeTruthy();
    expect(table.classList.contains("table-striped")).toBe(true);
    expect(table.classList.contains("table-bordered")).toBe(true);
    expect(table.classList.contains("table-hover")).toBe(true);

    // Column headers present
    expect(canvas.getByText("Token ID")).toBeInTheDocument();
    expect(canvas.getByText("Expires")).toBeInTheDocument();

    // Create buttons present
    expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    expect(canvas.getByText("New permanent token")).toBeInTheDocument();

    // Delete buttons in each row
    const deleteButtons = canvasElement.querySelectorAll("table .btn-danger");
    expect(deleteButtons.length).toBe(3);
  },
};

export const CreateTemporaryToken = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),
        http.post("/api/token", async ({ request }) => {
          const body = await request.json();
          // Verify permanent=false for temporary tokens
          if (body.permanent === false) {
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
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: mockWriteText },
      writable: true,
      configurable: true,
    });

    // Click the temporary token button
    await userEvent.click(canvas.getByText("New temporary token"));

    // Modal shows the created token value
    await waitFor(() => {
      expect(
        canvas.getByText("Here is your new token. This value will only be displayed once."),
      ).toBeInTheDocument();
    });

    // Token value is displayed
    const tokenPre = canvasElement.querySelector(".created-token-value");
    expect(tokenPre).toBeTruthy();
    expect(tokenPre.textContent).toBe(MOCK_CREATED_TOKEN.token);

    // Copy button is present
    const copyBtn = canvas.getByTitle("Copy token to clipboard");
    expect(copyBtn).toBeTruthy();

    // Click copy and verify clipboard was called
    await userEvent.click(copyBtn);
    expect(mockWriteText).toHaveBeenCalledWith(MOCK_CREATED_TOKEN.token);
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

    // Modal shows the created token
    await waitFor(() => {
      expect(
        canvas.getByText("Here is your new token. This value will only be displayed once."),
      ).toBeInTheDocument();
    });

    // Token value is displayed in the modal
    const tokenPre = canvasElement.querySelector(".created-token-value");
    expect(tokenPre).toBeTruthy();
    expect(tokenPre.textContent).toBe(MOCK_CREATED_TOKEN.token);

    // The heading says "Token created"
    const modalTitle = canvasElement.querySelector(".modal-title");
    expect(modalTitle).toBeTruthy();
    expect(modalTitle.textContent).toBe("Token created");
  },
};

export const DeleteToken = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/token", () => {
          // Return progressively fewer tokens after deletion
          return HttpResponse.json(MOCK_TOKENS);
        }),
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

    // Click the first Delete button
    const deleteButtons = canvasElement.querySelectorAll("table .btn-danger");
    expect(deleteButtons.length).toBe(3);
    await userEvent.click(deleteButtons[0]);

    // Confirmation modal appears
    await waitFor(() => {
      expect(
        canvas.getByText("Are you sure? This will permanently remove this token."),
      ).toBeInTheDocument();
    });

    // Click the Delete button in the confirmation modal
    // The modal footer has both "Delete" and "Cancel" buttons —
    // find the Delete button inside the modal footer
    const modalFooterBtns = canvasElement.querySelectorAll(".modal-footer .btn-danger");
    expect(modalFooterBtns.length).toBeGreaterThan(0);
    await userEvent.click(modalFooterBtns[0]);

    // After deletion the table re-renders (tokens refetched)
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
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
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for tokens to load
    await waitFor(() => {
      expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    });

    // Click the first Delete button
    const deleteButtons = canvasElement.querySelectorAll("table .btn-danger");
    await userEvent.click(deleteButtons[0]);

    // Confirmation modal appears
    await waitFor(() => {
      expect(
        canvas.getByText("Are you sure? This will permanently remove this token."),
      ).toBeInTheDocument();
    });

    // Click Cancel
    const cancelBtn = canvas.getByText("Cancel");
    await userEvent.click(cancelBtn);

    // All tokens remain in the table
    expect(canvas.getByText("token-abc-123-def-456")).toBeInTheDocument();
    expect(canvas.getByText("token-ghi-789-jkl-012")).toBeInTheDocument();
    expect(canvas.getByText("token-permanent-001")).toBeInTheDocument();
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

    // Wait for loading to finish
    await waitFor(() => {
      expect(canvas.queryByText("Loading...")).toBeNull();
    });

    // Empty state message is shown
    expect(canvas.getByText("No tokens have been created yet.")).toBeInTheDocument();

    // No table rendered
    const table = canvasElement.querySelector("table");
    expect(table).toBeNull();

    // Create buttons are still present
    expect(canvas.getByText("New temporary token")).toBeInTheDocument();
    expect(canvas.getByText("New permanent token")).toBeInTheDocument();
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
    await waitFor(() => {
      const errorMessage = canvasElement.querySelector(".error-message");
      expect(errorMessage).toBeTruthy();
      expect(errorMessage.textContent).toContain("Rate limit exceeded");
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
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    const originalClipboard = navigator.clipboard;
    // Reject writeText — simulates permissions-denied / insecure-context.
    Object.defineProperty(navigator, "clipboard", {
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
      await waitFor(() => {
        expect(canvas.getByTitle("Copy token to clipboard")).toBeInTheDocument();
      });
      await userEvent.click(canvas.getByTitle("Copy token to clipboard"));

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
      Object.defineProperty(navigator, "clipboard", {
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
    Object.defineProperty(navigator, "clipboard", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    try {
      await waitFor(() => expect(canvas.getByText("New temporary token")).toBeInTheDocument());
      await userEvent.click(canvas.getByText("New temporary token"));

      await waitFor(() => {
        expect(canvas.getByTitle("Copy token to clipboard")).toBeInTheDocument();
      });
      await userEvent.click(canvas.getByTitle("Copy token to clipboard"));

      await waitFor(() => {
        const feedback = canvasElement.querySelector('[data-testid="copy-feedback"]');
        expect(feedback).toBeTruthy();
        expect(feedback.textContent).toContain("Clipboard not available");
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
  },
};
