import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
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
    // assertions hold.
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

/**
 * Wait until the loading skeleton avatar is gone — the only DOM signal that
 * the auth probe has resolved (whether to an authenticated user or a 401).
 * @param {HTMLElement} canvasElement - The Storybook canvas root.
 */
async function waitForNavbar(canvasElement) {
  await waitFor(
    () => {
      const skel = canvasElement.querySelector(".cts-skel-avatar");
      expect(skel).toBeNull();
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
    await waitForNavbar(canvasElement);

    // All authenticated nav links visible.
    expect(canvas.getByText("Home")).toBeInTheDocument();
    expect(canvas.getByText("Create Test")).toBeInTheDocument();
    expect(canvas.getByText("Test Plans")).toBeInTheDocument();
    expect(canvas.getByText("Test Logs")).toBeInTheDocument();
    expect(canvas.getByText("API Docs")).toBeInTheDocument();

    // Tokens link visible (in nav AND inside the account menu).
    const tokensLinks = canvas.getAllByText("Tokens");
    expect(tokensLinks.length).toBeGreaterThanOrEqual(2);

    // Account menu trigger present and closed by default.
    const trigger = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".cts-account-trigger")
    );
    expect(trigger).toBeTruthy();
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    expect(trigger.getAttribute("aria-label")).toBe("Account menu for Test User");

    // User name + principal live inside the account menu (in DOM regardless
    // of open state — the menu uses opacity/pointer-events for closed state,
    // not display:none, so Testing Library still finds them).
    expect(canvas.getByText("Test User")).toBeInTheDocument();
    expect(canvas.getByText("testuser@example.com")).toBeInTheDocument();
    expect(canvas.getByText("Sign out")).toBeInTheDocument();

    // No ADMIN badge for a regular user.
    expect(canvas.queryByText("ADMIN")).toBeNull();

    // Avatar should NOT carry the admin ring.
    const avatar = canvasElement.querySelector(".cts-avatar");
    expect(avatar.classList.contains("is-admin")).toBe(false);

    // Home link should be active.
    const homeLink = canvas.getByText("Home");
    expect(homeLink.classList.contains("active")).toBe(true);

    // OpenID logo present.
    const logo = canvasElement.querySelector('img[alt="OpenID Foundation"]');
    expect(logo).toBeTruthy();
  },
};

export const Admin = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_ADMIN)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvasElement);

    // ADMIN badge visible inside the menu, AND surfaced on the avatar via
    // the rust ring class so it persists when the menu is closed.
    expect(canvas.getByText("ADMIN")).toBeInTheDocument();
    expect(canvas.getByText("Admin User")).toBeInTheDocument();
    const avatar = canvasElement.querySelector(".cts-avatar");
    expect(avatar.classList.contains("is-admin")).toBe(true);

    // Tokens nav link should be hidden for admin.
    const navLinks = canvasElement.querySelectorAll(".cts-navlink");
    const navLinkTexts = Array.from(navLinks).map((el) => el.textContent.trim());
    expect(navLinkTexts).not.toContain("Tokens");

    // Tokens menu item should also be hidden for admin.
    const tokensMenuItem = canvasElement.querySelector('.cts-account-item[href="tokens.html"]');
    expect(tokensMenuItem).toBeNull();

    // Sign out always available.
    expect(canvas.getByText("Sign out")).toBeInTheDocument();
  },
};

export const Guest = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_GUEST)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvasElement);

    // Guest user: no ADMIN badge.
    expect(canvas.queryByText("ADMIN")).toBeNull();
    expect(canvas.getByText("Guest User")).toBeInTheDocument();

    // Avatar has no admin ring.
    const avatar = canvasElement.querySelector(".cts-avatar");
    expect(avatar.classList.contains("is-admin")).toBe(false);

    // Tokens hidden in both nav AND menu for guest.
    const navLinks = canvasElement.querySelectorAll(".cts-navlink");
    const navLinkTexts = Array.from(navLinks).map((el) => el.textContent.trim());
    expect(navLinkTexts).not.toContain("Tokens");
    const tokensMenuItem = canvasElement.querySelector('.cts-account-item[href="tokens.html"]');
    expect(tokensMenuItem).toBeNull();
  },
};

export const Unauthenticated = {
  args: { currentPage: "" },
  decorators: [withUnauthenticated()],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvasElement);

    // Public-only nav links.
    expect(canvas.getByText("Published Logs")).toBeInTheDocument();
    expect(canvas.getByText("Published Plans")).toBeInTheDocument();
    expect(canvas.getByText("API Docs")).toBeInTheDocument();

    // Authenticated links absent.
    expect(canvas.queryByText("Home")).toBeNull();
    expect(canvas.queryByText("Create Test")).toBeNull();
    expect(canvas.queryByText("Test Plans")).toBeNull();
    expect(canvas.queryByText("Test Logs")).toBeNull();

    // No account menu, no Sign out.
    expect(canvasElement.querySelector(".cts-account")).toBeNull();
    expect(canvas.queryByText("Sign out")).toBeNull();

    // Sign in button present and points at the login page.
    const signIn = /** @type {HTMLAnchorElement} */ (canvas.getByText("Sign in"));
    expect(signIn).toBeInTheDocument();
    expect(signIn.getAttribute("href")).toBe("login.html");

    // Logo still present.
    const logo = canvasElement.querySelector('img[alt="OpenID Foundation"]');
    expect(logo).toBeTruthy();
  },
};

export const Loading = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER, { delay: 60000 })],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Loading uses a skeleton avatar — a 30x30 ink-700 disc — instead of a
    // text label that would shift layout when the user resolves. Wait for
    // it to appear (the auth fetch is delayed by the decorator).
    await waitFor(() => {
      const skel = canvasElement.querySelector(".cts-skel-avatar");
      expect(skel).toBeTruthy();
    });

    // Logo visible while loading.
    const logo = canvasElement.querySelector('img[alt="OpenID Foundation"]');
    expect(logo).toBeTruthy();

    // While loading, user is null so component shows public nav links.
    expect(canvas.queryByText("Home")).toBeNull();
    expect(canvas.getByText("Published Logs")).toBeInTheDocument();

    // No account trigger, no Sign in, no Sign out — only the skeleton.
    expect(canvasElement.querySelector(".cts-account-trigger")).toBeNull();
    expect(canvas.queryByText("Sign in")).toBeNull();
    expect(canvas.queryByText("Sign out")).toBeNull();
  },
};

export const ActivePagePlans = {
  args: { currentPage: "plans" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvasElement);

    const plansLink = canvas.getByText("Test Plans");
    expect(plansLink.classList.contains("active")).toBe(true);

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
    await waitForNavbar(canvasElement);

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
    await waitForNavbar(canvasElement);

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
    await waitForNavbar(canvasElement);

    const navbar = canvasElement.querySelector("cts-navbar");
    expect(navbar).toBeTruthy();

    // Initial: only Test Plans is active.
    let activeLinks = canvasElement.querySelectorAll(".cts-navlink.active");
    expect(activeLinks).toHaveLength(1);
    expect(canvas.getByText("Test Plans").classList.contains("active")).toBe(true);

    // Flip current-page; classMap must move `active` to the new link and
    // strip it from the previous one.
    navbar.setAttribute("current-page", "logs");
    await navbar.updateComplete;

    activeLinks = canvasElement.querySelectorAll(".cts-navlink.active");
    expect(activeLinks).toHaveLength(1);
    expect(canvas.getByText("Test Logs").classList.contains("active")).toBe(true);
    expect(canvas.getByText("Test Plans").classList.contains("active")).toBe(false);
  },
};

/**
 * Account menu opens on click and exposes ARIA state. Verifies the
 * common-pattern dropdown contract: aria-expanded flips, the popover
 * receives data-open="true", and menu items are reachable.
 */
export const AccountMenuOpens = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    await waitForNavbar(canvasElement);

    const account = canvasElement.querySelector(".cts-account");
    const trigger = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".cts-account-trigger")
    );
    expect(account.getAttribute("data-open")).toBe("false");
    expect(trigger.getAttribute("aria-expanded")).toBe("false");

    await userEvent.click(trigger);

    expect(account.getAttribute("data-open")).toBe("true");
    expect(trigger.getAttribute("aria-expanded")).toBe("true");

    // Tokens link in the open menu points at /tokens.html.
    const tokensItem = /** @type {HTMLAnchorElement} */ (
      canvasElement.querySelector('.cts-account-item[href="tokens.html"]')
    );
    expect(tokensItem).toBeTruthy();
    expect(tokensItem.getAttribute("role")).toBe("menuitem");

    // Sign out is a form submit button so the existing CSRF-bound POST
    // /logout flow keeps working — not a plain link.
    const signOutForm = /** @type {HTMLFormElement} */ (
      canvasElement.querySelector(".cts-account-form")
    );
    expect(signOutForm.getAttribute("action")).toBe("/logout");
    expect(signOutForm.getAttribute("method")).toBe("post");
  },
};

/**
 * Escape closes the menu and returns focus to the trigger. Matches native
 * popover/details behavior so keyboard users land back where they invoked
 * the menu.
 */
export const AccountMenuClosesOnEscape = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    await waitForNavbar(canvasElement);

    const account = canvasElement.querySelector(".cts-account");
    const trigger = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".cts-account-trigger")
    );
    await userEvent.click(trigger);
    expect(account.getAttribute("data-open")).toBe("true");

    await userEvent.keyboard("{Escape}");

    const navbar = canvasElement.querySelector("cts-navbar");
    await navbar.updateComplete;
    expect(account.getAttribute("data-open")).toBe("false");
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    // Focus returns to the trigger so screen-reader users keep their place.
    expect(document.activeElement).toBe(trigger);
  },
};

/**
 * A pointerdown anywhere outside the account dismisses the menu. This is
 * the common-pattern "click outside to close" affordance — required so the
 * menu doesn't trap users who change their mind after opening it.
 *
 * Dispatching the synthetic pointerdown directly on document.body avoids
 * userEvent.click on a real element (which would, e.g., follow the brand
 * link's href and tear down the Storybook iframe mid-test).
 */
export const AccountMenuClosesOnOutsideClick = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    await waitForNavbar(canvasElement);

    const account = canvasElement.querySelector(".cts-account");
    const trigger = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".cts-account-trigger")
    );
    await userEvent.click(trigger);
    expect(account.getAttribute("data-open")).toBe("true");

    document.body.dispatchEvent(new PointerEvent("pointerdown", { bubbles: true }));

    const navbar = canvasElement.querySelector("cts-navbar");
    await navbar.updateComplete;
    expect(account.getAttribute("data-open")).toBe("false");
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
  },
};

/**
 * The API Docs link opens in a new tab — it's a separate viewer the user
 * typically wants kept open while iterating on tests. Verifies the link
 * carries target="_blank", rel="noopener noreferrer", and the visible
 * external-link affordance icon.
 */
export const ApiDocsIsExternalLink = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    await waitForNavbar(canvasElement);

    const apiLink = /** @type {HTMLAnchorElement} */ (
      canvasElement.querySelector('.cts-navlink[href="api-document.html"]')
    );
    expect(apiLink).toBeTruthy();
    expect(apiLink.getAttribute("target")).toBe("_blank");
    expect(apiLink.getAttribute("rel")).toBe("noopener noreferrer");
    expect(apiLink.classList.contains("cts-navlink-external")).toBe(true);

    const icon = /** @type {SVGElement} */ (apiLink.querySelector(".cts-navlink-external-icon"));
    expect(icon).toBeTruthy();
    expect(icon.tagName.toLowerCase()).toBe("svg");
    expect(icon.getAttribute("aria-hidden")).toBe("true");

    // No other nav link should carry external-link signals — only API Docs.
    const otherInternalLink = /** @type {HTMLAnchorElement} */ (
      canvasElement.querySelector('.cts-navlink[href="plans.html"]')
    );
    expect(otherInternalLink.getAttribute("target")).toBe("_self");
    expect(otherInternalLink.querySelector(".cts-navlink-external-icon")).toBeNull();
  },
};

/**
 * Mobile hamburger toggles the nav-link panel. The link <ul> stays in
 * the same DOM position; the media query at ≤820px repositions it as
 * a popover and the [data-mobile-open] attribute on .cts-nav controls
 * its visibility. Verifies the ARIA contract on the toggle.
 *
 * Pins the viewport to mobile1 (320×568) so the rendered story actually
 * shows the mobile chrome (hamburger visible, wordmark hidden, nav
 * panel pinned to the navbar's bottom edge) rather than the desktop
 * layout where the toggle is display:none.
 */
export const MobileMenuTogglesNavlinks = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
  globals: {
    viewport: { value: "mobile1", isRotated: false },
  },
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    await waitForNavbar(canvasElement);

    const nav = /** @type {HTMLElement} */ (canvasElement.querySelector(".cts-nav"));
    const toggle = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".cts-menu-toggle")
    );
    expect(toggle).toBeTruthy();
    expect(toggle.getAttribute("aria-haspopup")).toBe("true");
    expect(toggle.getAttribute("aria-controls")).toBe("cts-navlinks");
    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(nav.getAttribute("data-mobile-open")).toBe("false");

    await userEvent.click(toggle);

    expect(toggle.getAttribute("aria-expanded")).toBe("true");
    expect(toggle.getAttribute("aria-label")).toBe("Close navigation menu");
    expect(nav.getAttribute("data-mobile-open")).toBe("true");

    // Toggle navlinks <ul> is the same element pre/post — the same DOM
    // node serves both layouts, so its id is consistent and the active
    // state is preserved across viewport changes.
    const navlinks = canvasElement.querySelector("#cts-navlinks");
    expect(navlinks).toBeTruthy();
    expect(navlinks.tagName).toBe("UL");

    await userEvent.click(toggle);
    expect(nav.getAttribute("data-mobile-open")).toBe("false");
    expect(toggle.getAttribute("aria-label")).toBe("Open navigation menu");
  },
};

/**
 * Escape and outside-pointerdown both close the mobile nav panel — same
 * affordance as the account menu. Escape additionally returns focus to
 * the hamburger toggle.
 *
 * Pinned to mobile1 (320×568) so the rendered story shows the actual
 * mobile chrome where the hamburger lives.
 */
export const MobileMenuClosesOnEscape = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
  globals: {
    viewport: { value: "mobile1", isRotated: false },
  },
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    await waitForNavbar(canvasElement);

    const nav = /** @type {HTMLElement} */ (canvasElement.querySelector(".cts-nav"));
    const toggle = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".cts-menu-toggle")
    );
    await userEvent.click(toggle);
    expect(nav.getAttribute("data-mobile-open")).toBe("true");

    await userEvent.keyboard("{Escape}");

    const navbar = canvasElement.querySelector("cts-navbar");
    await navbar.updateComplete;
    expect(nav.getAttribute("data-mobile-open")).toBe("false");
    expect(document.activeElement).toBe(toggle);
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
      await waitForNavbar(canvasElement);

      // Should fall back to the public nav — same as the unauthenticated case.
      expect(canvas.getByText("Published Logs")).toBeInTheDocument();
      expect(canvas.queryByText("Sign out")).toBeNull();
      expect(canvas.getByText("Sign in")).toBeInTheDocument();

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
 * preview structure. Asserts the scoped class names and the avatar element
 * are present so the design-token stylesheet (--ink-900 / --orange-400 /
 * etc.) actually applies.
 */
export const DesignSystemStructure = {
  args: { currentPage: "plans" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) => html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForNavbar(canvasElement);

    // Container uses the OIDF .cts-nav class — that's what the scoped
    // stylesheet selector keys off, so missing this class means the dark
    // chrome will not paint.
    const nav = canvasElement.querySelector("nav.cts-nav");
    expect(nav).toBeTruthy();

    // Brand block on the left. Logo is the dark-on-dark SVG; explicit
    // width/height attributes reserve the logo box so the brand row does
    // not reflow during the first paint.
    const brand = canvasElement.querySelector(".cts-nav .cts-brand");
    expect(brand).toBeTruthy();
    const brandLogo = /** @type {HTMLImageElement} */ (
      canvasElement.querySelector('.cts-brand img[alt="OpenID Foundation"]')
    );
    expect(brandLogo).toBeTruthy();
    expect(brandLogo.getAttribute("src")).toBe("/images/openid-dark.svg");
    expect(brandLogo.getAttribute("width")).toBe("93");
    expect(brandLogo.getAttribute("height")).toBe("28");
    expect(canvas.getByText("CONFORMANCE SUITE")).toBeInTheDocument();

    // Centered link block.
    const navlinks = canvasElement.querySelector(".cts-nav .cts-navlinks");
    expect(navlinks).toBeTruthy();
    const links = navlinks.querySelectorAll("a.cts-navlink");
    expect(links.length).toBeGreaterThanOrEqual(5);

    // Right-hand block: account zone with trigger button + popover menu.
    const navright = canvasElement.querySelector(".cts-nav .cts-navright");
    expect(navright).toBeTruthy();
    const account = canvasElement.querySelector(".cts-nav .cts-account");
    expect(account).toBeTruthy();
    expect(account.getAttribute("data-open")).toBe("false");

    const trigger = canvasElement.querySelector(".cts-account-trigger");
    expect(trigger).toBeTruthy();
    expect(trigger.getAttribute("aria-haspopup")).toBe("true");
    expect(trigger.getAttribute("aria-controls")).toBe("cts-account-menu");

    const avatar = canvasElement.querySelector(".cts-account-trigger .cts-avatar");
    expect(avatar).toBeTruthy();
    // Initials fall back to two letters from the display name ("Test User" -> "TU").
    expect(avatar.textContent.trim()).toBe("TU");

    const menu = canvasElement.querySelector("#cts-account-menu");
    expect(menu).toBeTruthy();
    expect(menu.getAttribute("role")).toBe("menu");
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
      await waitForNavbar(canvasElement);

      expect(canvas.getByText("Published Logs")).toBeInTheDocument();
      const currentuserWarns = warnSpy.mock.calls
        .flat()
        .filter((arg) => typeof arg === "string" && arg.includes("/api/currentuser"));
      expect(currentuserWarns).toEqual([]);
    } finally {
      console.warn = origWarn;
    }
  },
};
