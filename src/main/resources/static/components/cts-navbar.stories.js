import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
import "./cts-navbar.js";

export default {
  title: "Components/cts-navbar",
  component: "cts-navbar",
  argTypes: {
    currentPage: {
      control: "select",
      options: ["home", "create-test", "plans", "logs", "tokens", "api-docs"],
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
 * Also clears sessionStorage so cts-navbar's user cache does not leak the
 * previous story's mock response into the next story.
 */
function withMockUser(mockResponse, { delay = 0, status = 200 } = {}) {
  return (storyFn) => {
    // Keep stories hermetic: cts-navbar reads /api/currentuser result from
    // sessionStorage on first render to stabilize across real-app page
    // navigations; inside Storybook, each story must start clean so its
    // assertions (e.g. "Loading…" state in the Loading story) hold.
    try {
      sessionStorage.clear();
    } catch {
      // Storybook sometimes runs in sandboxed contexts where sessionStorage
      // is unavailable; the stories still render, just without isolation
      // guarantees when run in that mode.
    }
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
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

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
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // ADMIN badge visible
    expect(canvas.getByText("ADMIN")).toBeInTheDocument();
    expect(canvas.getByText("Admin User")).toBeInTheDocument();

    // Tokens nav link should be hidden for admin
    const navLinks = canvasElement.querySelectorAll(".nav-link");
    const navLinkTexts = Array.from(navLinks).map((el) => el.textContent.trim());
    expect(navLinkTexts).not.toContain("Tokens");

    // Tokens button in user info area should also be hidden
    const tokensBtn = canvasElement.querySelector('a.btn[href="tokens.html"]');
    expect(tokensBtn).toBeNull();

    // Logout still visible
    expect(canvas.getByText("Logout")).toBeInTheDocument();
  },
};

export const Guest = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_GUEST)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // Guest user: no ADMIN badge
    expect(canvas.queryByText("ADMIN")).toBeNull();
    expect(canvas.getByText("Guest User")).toBeInTheDocument();

    // Tokens nav link hidden for guest
    const navLinks = canvasElement.querySelectorAll(".nav-link");
    const navLinkTexts = Array.from(navLinks).map((el) => el.textContent.trim());
    expect(navLinkTexts).not.toContain("Tokens");

    // Tokens button in user info area also hidden
    const tokensBtn = canvasElement.querySelector('a.btn[href="tokens.html"]');
    expect(tokensBtn).toBeNull();
  },
};

export const Unauthenticated = {
  args: { currentPage: "" },
  decorators: [withUnauthenticated()],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

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
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

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
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

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
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

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
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    const createLink = canvas.getByText("Create Test");
    expect(createLink.classList.contains("active")).toBe(true);

    const homeLink = canvas.getByText("Home");
    expect(homeLink.classList.contains("active")).toBe(false);
  },
};

/**
 * Guards classMap-driven active-class toggle across a live `current-page`
 * attribute change. Exercises the path that motivated adopting classMap:
 * only one link should carry `active` after the attribute flips.
 */
export const ActivePageTransition = {
  args: { currentPage: "plans" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    const navbar = canvasElement.querySelector("cts-navbar");
    expect(navbar).toBeTruthy();

    // Initial: only Test Plans is active.
    let activeLinks = canvasElement.querySelectorAll(".nav-link.active");
    expect(activeLinks).toHaveLength(1);
    expect(canvas.getByText("Test Plans").classList.contains("active")).toBe(true);

    // Flip current-page; classMap must move `active` to the new link and
    // strip it from the previous one.
    navbar.setAttribute("current-page", "logs");
    await navbar.updateComplete;

    activeLinks = canvasElement.querySelectorAll(".nav-link.active");
    expect(activeLinks).toHaveLength(1);
    expect(canvas.getByText("Test Logs").classList.contains("active")).toBe(true);
    expect(canvas.getByText("Test Plans").classList.contains("active")).toBe(false);
  },
};

/**
 * Non-401 failure from /api/currentuser (e.g. gateway 502, backend 500):
 * the navbar still renders the public nav (it's chrome — no better fallback)
 * but logs a warning so the failure is diagnosable.
 */
export const ServerErrorLogsWarning = {
  args: { currentPage: "" },
  decorators: [withMockUser(null, { status: 500 })],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      await waitForNavbar(canvas);

      // Should fall back to the public nav — same as the unauthenticated case.
      expect(canvas.getByText("Published Logs")).toBeInTheDocument();
      expect(canvas.queryByText("Logged in as")).toBeNull();

      // Unlike 401, a 500 must warn so operators see the failure.
      await waitFor(() => {
        expect(warnSpy).toHaveBeenCalled();
        const joined = warnSpy.mock.calls.flat().join(" ");
        expect(joined).toContain("cts-navbar");
        expect(joined).toContain("/api/currentuser");
        expect(joined).toContain("500");
      });
    } finally {
      console.warn = origWarn;
    }
  },
};

/**
 * Visual contract: the rendered DOM matches the OIDF design system navbar
 * preview structure (`project/preview/components-navbar.html`). Asserts the
 * scoped class names and the avatar element are present so the design-token
 * stylesheet (--ink-900 / --orange-400 / etc.) actually applies.
 */
export const DesignSystemStructure = {
  args: { currentPage: "plans" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvas);

    // Container uses the OIDF .cts-nav class — that's what the scoped
    // stylesheet selector keys off, so missing this class means the dark
    // chrome will not paint.
    const nav = canvasElement.querySelector("nav.cts-nav");
    expect(nav).toBeTruthy();

    // Brand block on the left.
    const brand = canvasElement.querySelector(".cts-nav .cts-brand");
    expect(brand).toBeTruthy();
    expect(canvasElement.querySelector('.cts-brand img[alt="OpenID"]')).toBeTruthy();
    expect(canvas.getByText("CONFORMANCE SUITE")).toBeInTheDocument();

    // Centered link block.
    const navlinks = canvasElement.querySelector(".cts-nav .cts-navlinks");
    expect(navlinks).toBeTruthy();
    const links = navlinks.querySelectorAll("a.cts-navlink");
    expect(links.length).toBeGreaterThanOrEqual(5);

    // Right-hand block with the avatar circle.
    const navright = canvasElement.querySelector(".cts-nav .cts-navright");
    expect(navright).toBeTruthy();
    const avatar = canvasElement.querySelector(".cts-nav .cts-avatar");
    expect(avatar).toBeTruthy();
    // Initials fall back to two letters from the display name ("Test User" -> "TU").
    expect(avatar.textContent.trim()).toBe("TU");
  },
};

/**
 * FOUC: the `:not(:defined)` fallback in css/layout.css must reserve 60px of
 * vertical space so content below the navbar does not shift when the custom
 * element finally upgrades. Verifies the rule both selects cts-navbar hosts
 * and applies the 60px min-height — see
 * docs/solutions/web-components/cts-navbar-inline-visibility-bug-2026-04-24.md.
 */
export const FoucFallbackReservesHeight = {
  args: { currentPage: "" },
  decorators: [withUnauthenticated()],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play() {
    // The fallback rule lives in css/layout.css (loaded by preview-head.html
    // for every story). Locate it and assert it both reserves a block-level
    // box AND a 60px height — both are required for the rule to actually
    // reserve vertical space on a custom-element host. See
    // docs/solutions/web-components/cts-navbar-inline-visibility-bug-2026-04-24.md
    // for why visibility:hidden alone is a silent no-op.
    const sheet = Array.from(document.styleSheets).find((s) =>
      (s.href || "").includes("/css/layout.css"),
    );
    expect(sheet).toBeTruthy();
    const cssRules = /** @type {CSSStyleRule[]} */ (
      Array.from(/** @type {CSSStyleSheet} */ (sheet).cssRules)
    );
    const fallback = cssRules.find(
      (r) => r.selectorText && r.selectorText.includes("cts-navbar:not(:defined)"),
    );
    expect(fallback).toBeTruthy();
    const fb = /** @type {CSSStyleRule} */ (fallback);
    expect(fb.style.minHeight).toBe("60px");
    expect(fb.style.display).toBe("block");
    expect(fb.style.visibility).toBe("hidden");
  },
};

/**
 * 401 is the expected response when the user is not logged in. The navbar
 * must NOT warn — it would spam the console on every anonymous page load.
 */
export const UnauthenticatedNoWarn = {
  args: { currentPage: "" },
  decorators: [withUnauthenticated()],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      await waitForNavbar(canvas);

      // Public nav should render as usual…
      expect(canvas.getByText("Published Logs")).toBeInTheDocument();
      // …and no warn should have been emitted for the expected 401.
      const currentuserWarns = warnSpy.mock.calls
        .flat()
        .filter((arg) => typeof arg === "string" && arg.includes("/api/currentuser"));
      expect(currentuserWarns).toEqual([]);
    } finally {
      console.warn = origWarn;
    }
  },
};
