import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import "./cts-login-page.js";
import "./cts-link-button.js";

export default {
  title: "Pages/cts-login-page",
  component: "cts-login-page",
  argTypes: {
    error: { control: "text" },
    logoutMessage: { control: "boolean" },
    tokenAuthUrl: { control: "text" },
  },
};

// --- Stories ---

export const Default = {
  render: () => html`<cts-login-page></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Page heading present
    await waitFor(() => {
      expect(
        canvas.getByText("Login to or Register with the OpenID Foundation Conformance Suite"),
      ).toBeInTheDocument();
    });

    // Google OAuth button renders with correct href
    const googleBtn = canvas.getByText("Proceed with Google");
    expect(googleBtn).toBeInTheDocument();
    expect(googleBtn.closest("a").getAttribute("href")).toBe("/oauth2/authorization/google");
    expect(googleBtn.closest("a").classList.contains("btn-danger")).toBe(true);

    // GitLab OAuth button renders with correct href
    const gitlabBtn = canvas.getByText("Proceed with GitLab");
    expect(gitlabBtn).toBeInTheDocument();
    expect(gitlabBtn.closest("a").getAttribute("href")).toBe("/oauth2/authorization/gitlab");
    expect(gitlabBtn.closest("a").classList.contains("btn-primary")).toBe(true);

    // Public links present
    expect(canvas.getByText("View published logs")).toBeInTheDocument();
    expect(canvas.getByText("View published plans")).toBeInTheDocument();

    // Public links have correct hrefs
    const logsLink = canvas.getByText("View published logs").closest("a");
    expect(logsLink.getAttribute("href")).toBe("logs.html?public=true");

    const plansLink = canvas.getByText("View published plans").closest("a");
    expect(plansLink.getAttribute("href")).toBe("plans.html?public=true");

    // No error or logout messages visible
    expect(canvas.queryByRole("alert")).toBeNull();
    expect(canvas.queryByRole("status")).toBeNull();

    // No hidden iframe
    const iframe = canvasElement.querySelector("iframe");
    expect(iframe).toBeNull();
  },
};

export const WithError = {
  render: () => html`<cts-login-page error="Invalid credentials"></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Error alert is visible
    await waitFor(() => {
      const alert = canvas.getByRole("alert");
      expect(alert).toBeInTheDocument();
    });

    // Error alert contains the error text
    const alert = canvas.getByRole("alert");
    expect(alert.textContent).toContain("There was an error logging you in:");
    expect(alert.textContent).toContain("Invalid credentials");
    expect(alert.classList.contains("bg-danger")).toBe(true);

    // Error details are in the dedicated span
    const details = alert.querySelector(".error-details");
    expect(details).toBeTruthy();
    expect(details.textContent).toBe("Invalid credentials");

    // No logout message
    expect(canvas.queryByRole("status")).toBeNull();

    // OAuth buttons still render
    expect(canvas.getByText("Proceed with Google")).toBeInTheDocument();
    expect(canvas.getByText("Proceed with GitLab")).toBeInTheDocument();
  },
};

export const PostLogout = {
  render: () => html`<cts-login-page logout-message></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Logout message is visible
    await waitFor(() => {
      const status = canvas.getByRole("status");
      expect(status).toBeInTheDocument();
    });

    const status = canvas.getByRole("status");
    expect(status.textContent).toContain("You have been logged out.");
    expect(status.classList.contains("bg-info")).toBe(true);

    // No error message
    expect(canvas.queryByRole("alert")).toBeNull();

    // OAuth buttons still render
    expect(canvas.getByText("Proceed with Google")).toBeInTheDocument();
    expect(canvas.getByText("Proceed with GitLab")).toBeInTheDocument();
  },
};

export const TokenAuth = {
  render: () => html`<cts-login-page token-auth-url="/login/ott?token=abc123"></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Hidden iframe is present for OTT auth
    await waitFor(() => {
      const iframe = canvasElement.querySelector("iframe");
      expect(iframe).toBeTruthy();
    });

    const iframe = canvasElement.querySelector("iframe");
    expect(iframe.getAttribute("src")).toBe("/login/ott?token=abc123");
    expect(iframe.style.display).toBe("none");
    expect(iframe.getAttribute("title")).toBe("Token authentication");

    // Page heading still visible
    expect(
      canvas.getByText("Login to or Register with the OpenID Foundation Conformance Suite"),
    ).toBeInTheDocument();

    // No error or logout messages
    expect(canvas.queryByRole("alert")).toBeNull();
    expect(canvas.queryByRole("status")).toBeNull();
  },
};

export const ErrorAndLogout = {
  render: () => html`<cts-login-page error="Session expired" logout-message></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Both error and logout messages are visible
    await waitFor(() => {
      expect(canvas.getByRole("alert")).toBeInTheDocument();
      expect(canvas.getByRole("status")).toBeInTheDocument();
    });

    // Error alert contains the error text
    const alert = canvas.getByRole("alert");
    expect(alert.textContent).toContain("Session expired");
    expect(alert.classList.contains("bg-danger")).toBe(true);

    // Logout message is also shown
    const status = canvas.getByRole("status");
    expect(status.textContent).toContain("You have been logged out.");
    expect(status.classList.contains("bg-info")).toBe(true);

    // OAuth buttons still render
    expect(canvas.getByText("Proceed with Google")).toBeInTheDocument();
    expect(canvas.getByText("Proceed with GitLab")).toBeInTheDocument();

    // Public links still present
    expect(canvas.getByText("View published logs")).toBeInTheDocument();
    expect(canvas.getByText("View published plans")).toBeInTheDocument();
  },
};
