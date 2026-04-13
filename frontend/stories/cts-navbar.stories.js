import { html } from "lit";
import "../../src/main/resources/static/components/cts-navbar.js";

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

    // Each render gets a fresh element so connectedCallback re-fires
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

// --- Stories ---

export const Authenticated = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const Admin = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_ADMIN)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const Guest = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_GUEST)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const Unauthenticated = {
  args: { currentPage: "" },
  decorators: [withUnauthenticated()],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const Loading = {
  args: { currentPage: "home" },
  decorators: [withMockUser(MOCK_USER, { delay: 60000 })],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const ActivePagePlans = {
  args: { currentPage: "plans" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const ActivePageLogs = {
  args: { currentPage: "logs" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};

export const ActivePageCreateTest = {
  args: { currentPage: "create-test" },
  decorators: [withMockUser(MOCK_USER)],
  render: ({ currentPage }) =>
    html`<cts-navbar current-page="${currentPage}"></cts-navbar>`,
};
