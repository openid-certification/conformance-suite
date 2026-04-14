import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import "./cts-navbar.js";

export default {
  title: "Components/cts-navbar",
  component: "cts-navbar",
  argTypes: {
    currentPage: {
      control: "select",
      options: [
        "home",
        "create-test",
        "plans",
        "logs",
        "tokens",
        "api-docs",
      ],
    },
  },
};

// --- Fetch mocking helpers ---

const MOCK_USER = {
  iss: "https://accounts.google.com",
  sub: "12345",
  principal: "testuser@example.com",
  displayName: "Test User",
  isAdmin: false,
  isGuest: false,
};

const MOCK_ADMIN = {
  ...MOCK_USER,
  displayName: "Admin User",
  isAdmin: true,
};

const MOCK_GUEST = {
  ...MOCK_USER,
  displayName: "Guest User",
  isGuest: true,
};

/**
 * Creates a decorator that intercepts fetch("/api/currentuser") with a
 * configurable response, and restores the real fetch after the story unmounts.
 */
function withMockUser(mockResponse, { delay = 0, status = 200 } = {}) {
  return (storyFn) => {
    const realFetch = window.fetch;
    window.fetch = (url, opts) => {
      if (typeof url === "string" && url.includes("/api/currentuser")) {
        return new Promise((resolve) =>
          setTimeout(
            () =>
              resolve(
                new Response(JSON.stringify(mockResponse), {
                  status,
                  headers: { "Content-Type": "application/json" },
                }),
              ),
            delay,
          ),
        );
      }
      return realFetch(url, opts);
    };

    const result = storyFn();

    // Restore after a tick so connectedCallback has fired
    queueMicrotask(() => {
      window.fetch = realFetch;
    });

    return result;
  };
}

function withUnauthenticated() {
  return withMockUser(null, { status: 401 });
}

// --- Helper to wait for component to finish loading ---

async function waitForNavbar(canvas) {
  // Wait until the loading indicator disappears (component finished fetching)
  await waitFor(
    () => {
      const loading = canvas.queryByText("Loading…");
      expect(loading).toBeNull();
    },
    { timeout: 3000 },
  );
}

// --- Stories ---

export const Authenticated = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // Should show all authenticated nav links
    expect(canvas.getByText("Home")).toBeInTheDocument();
    expect(canvas.getByText("Create Test")).toBeInTheDocument();
    expect(canvas.getByText("Test Plans")).toBeInTheDocument();
    expect(canvas.getByText("Test Logs")).toBeInTheDocument();
    expect(canvas.getByText("API Docs")).toBeInTheDocument();

    // Tokens nav link visible for regular user
    const tokensLinks = canvas.getAllByText("Tokens");
    expect(tokensLinks.length).toBeGreaterThanOrEqual(1);

    // User info visible
    expect(canvas.getByText("Logged in as")).toBeInTheDocument();
    expect(canvas.getByText("Test User")).toBeInTheDocument();
    expect(canvas.getByText("Logout")).toBeInTheDocument();

    // No ADMIN badge
    expect(canvas.queryByText("ADMIN")).toBeNull();

    // Home link should be active
    const homeLink = canvas.getByText("Home");
    expect(homeLink.classList.contains("active")).toBe(true);

    // OpenID logo present
    const logo = canvasElement.querySelector('img[alt="OpenID"]');
    expect(logo).toBeTruthy();
  },
};

export const Admin = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_ADMIN)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // ADMIN badge visible
    expect(canvas.getByText("ADMIN")).toBeInTheDocument();
    expect(canvas.getByText("Admin User")).toBeInTheDocument();

    // Tokens nav link should be hidden for admin
    const navLinks = canvasElement.querySelectorAll(".nav-link");
    const navLinkTexts = Array.from(navLinks).map((el) =>
      el.textContent.trim(),
    );
    expect(navLinkTexts).not.toContain("Tokens");

    // Tokens button in user info area should also be hidden
    const tokensBtn = canvasElement.querySelector(
      'a.btn[href="tokens.html"]',
    );
    expect(tokensBtn).toBeNull();

    // Logout still visible
    expect(canvas.getByText("Logout")).toBeInTheDocument();
  },
};

export const Guest = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_GUEST)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // Guest user: no ADMIN badge
    expect(canvas.queryByText("ADMIN")).toBeNull();
    expect(canvas.getByText("Guest User")).toBeInTheDocument();

    // Tokens nav link hidden for guest
    const navLinks = canvasElement.querySelectorAll(".nav-link");
    const navLinkTexts = Array.from(navLinks).map((el) =>
      el.textContent.trim(),
    );
    expect(navLinkTexts).not.toContain("Tokens");

    // Tokens button in user info area also hidden
    const tokensBtn = canvasElement.querySelector(
      'a.btn[href="tokens.html"]',
    );
    expect(tokensBtn).toBeNull();
  },
};

export const Unauthenticated = {
  args: { currentPage: "" },
  decorators: [withUnauthenticated()],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // Should show public-only nav links
    expect(canvas.getByText("Published Logs")).toBeInTheDocument();
    expect(canvas.getByText("Published Plans")).toBeInTheDocument();
    expect(canvas.getByText("API Docs")).toBeInTheDocument();

    // Should NOT show authenticated links
    expect(canvas.queryByText("Home")).toBeNull();
    expect(canvas.queryByText("Create Test")).toBeNull();
    expect(canvas.queryByText("Test Plans")).toBeNull();
    expect(canvas.queryByText("Test Logs")).toBeNull();

    // No user info, no logout
    expect(canvas.queryByText("Logged in as")).toBeNull();
    expect(canvas.queryByText("Logout")).toBeNull();

    // Logo still present
    const logo = canvasElement.querySelector('img[alt="OpenID"]');
    expect(logo).toBeTruthy();
  },
};

export const Loading = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER, { delay: 60000 })],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Should show loading state immediately
    await waitFor(() => {
      expect(canvas.getByText("Loading…")).toBeInTheDocument();
    });

    // Logo visible even while loading
    const logo = canvasElement.querySelector('img[alt="OpenID"]');
    expect(logo).toBeTruthy();

    // While loading, user is null so component shows public nav links
    expect(canvas.queryByText("Home")).toBeNull();
    expect(canvas.getByText("Published Logs")).toBeInTheDocument();

    // No user info rendered yet (only the "Loading…" indicator)
    expect(canvas.queryByText("Logged in as")).toBeNull();
    expect(canvas.queryByText("Logout")).toBeNull();
  },
};

export const ActivePagePlans = {
  args: { currentPage: "plans" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // Test Plans should be active
    const plansLink = canvas.getByText("Test Plans");
    expect(plansLink.classList.contains("active")).toBe(true);

    // Home should NOT be active
    const homeLink = canvas.getByText("Home");
    expect(homeLink.classList.contains("active")).toBe(false);
  },
};

export const ActivePageLogs = {
  args: { currentPage: "logs" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    const logsLink = canvas.getByText("Test Logs");
    expect(logsLink.classList.contains("active")).toBe(true);

    const homeLink = canvas.getByText("Home");
    expect(homeLink.classList.contains("active")).toBe(false);
  },
};

export const ActivePageCreateTest = {
  args: { currentPage: "create-test" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    const createLink = canvas.getByText("Create Test");
    expect(createLink.classList.contains("active")).toBe(true);

    const homeLink = canvas.getByText("Home");
    expect(homeLink.classList.contains("active")).toBe(false);
  },
};
