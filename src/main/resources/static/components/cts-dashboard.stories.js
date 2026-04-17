import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_SERVER_INFO } from "@fixtures/mock-test-data.js";
import "./cts-dashboard.js";

export default {
  title: "Pages/cts-dashboard",
  component: "cts-dashboard",
};

// --- Stories ---

export const Authenticated = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // All 6 cards should be visible for authenticated users
    expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
    expect(canvas.getByText("View my test logs")).toBeInTheDocument();
    expect(canvas.getByText("View my test plans")).toBeInTheDocument();
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Verify hrefs on cts-link-button anchors
    const links = canvasElement.querySelectorAll("cts-link-button");
    expect(links.length).toBe(6);

    const hrefs = Array.from(links).map((btn) =>
      btn.querySelector("a")?.getAttribute("href"),
    );
    expect(hrefs).toContain("schedule-test.html");
    expect(hrefs).toContain("logs.html");
    expect(hrefs).toContain("plans.html");
    expect(hrefs).toContain("logs.html?public=true");
    expect(hrefs).toContain("plans.html?public=true");
    expect(hrefs).toContain("api-document.html");

    // Footer text present
    expect(
      canvas.getByText("OpenID Foundation conformance suite"),
    ).toBeInTheDocument();
  },
};

export const ServerInfo = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for server info to appear after fetch
    await waitFor(
      () => {
        expect(canvas.getByText(/Version:/)).toBeInTheDocument();
      },
      { timeout: 3000 },
    );

    // Verify individual server info fields rendered
    const versionEl = canvasElement.querySelector("#serverinfo-version");
    expect(versionEl).toBeTruthy();
    expect(versionEl.textContent).toBe(MOCK_SERVER_INFO.version);

    const buildTimeEl = canvasElement.querySelector("#serverinfo-build_time");
    expect(buildTimeEl).toBeTruthy();
    expect(buildTimeEl.textContent).toBe(MOCK_SERVER_INFO.build_time);

    const revisionEl = canvasElement.querySelector("#serverinfo-revision");
    expect(revisionEl).toBeTruthy();
    expect(revisionEl.textContent).toBe(MOCK_SERVER_INFO.revision);
  },
};

export const Unauthenticated = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
      ],
    },
  },
  render: () =>
    html`<cts-dashboard .isAuthenticated=${false}></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Auth-only cards should be hidden
    expect(canvas.queryByText("Create a new test plan")).toBeNull();
    expect(canvas.queryByText("View my test logs")).toBeNull();
    expect(canvas.queryByText("View my test plans")).toBeNull();

    // Public cards should be visible
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Only 3 link buttons rendered
    const links = canvasElement.querySelectorAll("cts-link-button");
    expect(links.length).toBe(3);

    // Footer still present
    expect(
      canvas.getByText("OpenID Foundation conformance suite"),
    ).toBeInTheDocument();
  },
};

export const ServerInfoError = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () =>
          new HttpResponse(null, { status: 500 }),
        ),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // The server-info failure is non-critical, but silently swallowing it was
    // the bug — we should see a console.warn so operators can diagnose.
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      // Cards should still render despite server info error
      expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
      expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
      expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

      // All 6 cards present (authenticated by default)
      const links = canvasElement.querySelectorAll("cts-link-button");
      expect(links.length).toBe(6);

      // Footer text still present
      expect(
        canvas.getByText("OpenID Foundation conformance suite"),
      ).toBeInTheDocument();

      // Wait a tick for the fetch to complete, then verify no server info rendered
      await waitFor(
        () => {
          const serverInfoDiv = canvasElement.querySelector(".serverInfo");
          // The div exists but should have no version info inside
          expect(serverInfoDiv.querySelector("#serverinfo-version")).toBeNull();
        },
        { timeout: 3000 },
      );

      // The 500 should have produced a console.warn mentioning the endpoint.
      await waitFor(() => {
        expect(warnSpy).toHaveBeenCalled();
        const joined = warnSpy.mock.calls.flat().join(" ");
        expect(joined).toContain("cts-dashboard");
        expect(joined).toContain("/api/server");
      });
    } finally {
      console.warn = origWarn;
    }
  },
};

export const Loading = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", async () => {
          // Simulate a very long delay — server info never arrives
          await new Promise(() => {});
          return HttpResponse.json(MOCK_SERVER_INFO);
        }),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Cards render immediately, regardless of server info loading
    expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
    expect(canvas.getByText("View my test logs")).toBeInTheDocument();
    expect(canvas.getByText("View my test plans")).toBeInTheDocument();
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Footer text is present
    expect(
      canvas.getByText("OpenID Foundation conformance suite"),
    ).toBeInTheDocument();

    // Server info has not loaded yet — no version info displayed
    const versionEl = canvasElement.querySelector("#serverinfo-version");
    expect(versionEl).toBeNull();
  },
};
