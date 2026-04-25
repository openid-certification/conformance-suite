import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import "./cts-login-page.js";
import "./cts-link-button.js";
import "./cts-alert.js";

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

    // Form heading present
    await waitFor(() => {
      expect(canvas.getByText("Sign in to continue")).toBeInTheDocument();
    });

    // Two-pane shell renders on the muted background
    const wrapper = canvasElement.querySelector(".oidf-login-page");
    expect(wrapper).toBeTruthy();
    const card = canvasElement.querySelector(".oidf-login-card");
    expect(card).toBeTruthy();

    // Brand band renders the OIDF wordmark and capability pillars
    const brand = canvasElement.querySelector(".oidf-login-brand");
    expect(brand).toBeTruthy();
    const brandLogo = /** @type {HTMLImageElement} */ (
      canvasElement.querySelector(".oidf-login-brand__logo")
    );
    expect(brandLogo).toBeTruthy();
    expect(brandLogo.getAttribute("src")).toBe("/images/openid-dark.svg");
    expect(brandLogo.getAttribute("alt")).toBe("OpenID");
    expect(canvasElement.querySelectorAll(".oidf-login-brand__pillars li").length).toBe(4);

    // Google OAuth button renders with correct href via cts-link-button -> <a>
    const googleAnchor = /** @type {HTMLAnchorElement} */ (
      canvas.getByText("Proceed with Google").closest("a")
    );
    expect(googleAnchor).toBeTruthy();
    expect(googleAnchor.getAttribute("href")).toBe("/oauth2/authorization/google");
    // Token-styled button class — Bootstrap btn-* must NOT leak through.
    expect(googleAnchor.classList.contains("oidf-btn")).toBe(true);
    expect(googleAnchor.classList.contains("oidf-btn-secondary")).toBe(true);
    expect(googleAnchor.classList.contains("btn-danger")).toBe(false);
    expect(googleAnchor.classList.contains("btn")).toBe(false);
    // Vendor mark precedes the label
    expect(googleAnchor.querySelector(".bi.bi-google")).toBeTruthy();

    // GitLab OAuth button renders with correct href
    const gitlabAnchor = /** @type {HTMLAnchorElement} */ (
      canvas.getByText("Proceed with GitLab").closest("a")
    );
    expect(gitlabAnchor).toBeTruthy();
    expect(gitlabAnchor.getAttribute("href")).toBe("/oauth2/authorization/gitlab");
    expect(gitlabAnchor.classList.contains("oidf-btn")).toBe(true);
    expect(gitlabAnchor.classList.contains("oidf-btn-secondary")).toBe(true);
    expect(gitlabAnchor.classList.contains("btn-primary")).toBe(false);
    expect(gitlabAnchor.querySelector(".bi.bi-gitlab")).toBeTruthy();

    // Public links present
    expect(canvas.getByText("View published logs")).toBeInTheDocument();
    expect(canvas.getByText("View published plans")).toBeInTheDocument();

    // Public links have correct hrefs and live in rich-list anchors
    const logsLink = /** @type {HTMLAnchorElement} */ (
      canvas.getByText("View published logs").closest("a")
    );
    expect(logsLink.getAttribute("href")).toBe("logs.html?public=true");
    expect(logsLink.classList.contains("oidf-login-link")).toBe(true);

    const plansLink = /** @type {HTMLAnchorElement} */ (
      canvas.getByText("View published plans").closest("a")
    );
    expect(plansLink.getAttribute("href")).toBe("plans.html?public=true");
    expect(plansLink.classList.contains("oidf-login-link")).toBe(true);

    // No error or logout cts-alert renders by default
    expect(canvasElement.querySelector("cts-alert")).toBeNull();

    // No hidden iframe
    const iframe = canvasElement.querySelector("iframe");
    expect(iframe).toBeNull();

    // Bootstrap layout classes must NOT leak through.
    expect(canvasElement.querySelector(".container-fluid")).toBeNull();
    expect(canvasElement.querySelector(".row")).toBeNull();
  },
};

export const WithError = {
  render: () => html`<cts-login-page error="Invalid credentials"></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Error alert is visible — the inner div carries role="alert" from cts-alert.
    await waitFor(() => {
      const alert = canvasElement.querySelector(".oidf-alert-danger");
      expect(alert).toBeTruthy();
    });

    const alert = /** @type {HTMLElement} */ (canvasElement.querySelector(".oidf-alert-danger"));
    expect(alert.getAttribute("role")).toBe("alert");
    expect(alert.textContent).toContain("There was an error logging you in:");
    expect(alert.textContent).toContain("Invalid credentials");
    // Bootstrap bg-* must NOT leak through.
    expect(alert.classList.contains("bg-danger")).toBe(false);

    // Error details are in the dedicated span
    const details = alert.querySelector(".error-details");
    expect(details).toBeTruthy();
    expect(/** @type {Element} */ (details).textContent).toBe("Invalid credentials");

    // No logout cts-alert
    expect(canvasElement.querySelector(".oidf-alert-info")).toBeNull();

    // OAuth buttons still render
    expect(canvas.getByText("Proceed with Google")).toBeInTheDocument();
    expect(canvas.getByText("Proceed with GitLab")).toBeInTheDocument();
  },
};

export const PostLogout = {
  render: () => html`<cts-login-page logout-message></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Logout message is rendered inside a cts-alert with the info variant.
    await waitFor(() => {
      const alert = canvasElement.querySelector(".oidf-alert-info");
      expect(alert).toBeTruthy();
    });

    const host = canvasElement.querySelector("cts-alert[variant='info']");
    expect(host).toBeTruthy();

    const alert = /** @type {HTMLElement} */ (canvasElement.querySelector(".oidf-alert-info"));
    expect(alert.textContent).toContain("You have been logged out.");
    // Bootstrap bg-info must NOT leak through.
    expect(alert.classList.contains("bg-info")).toBe(false);

    // No error cts-alert
    expect(canvasElement.querySelector(".oidf-alert-danger")).toBeNull();

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

    const iframe = /** @type {HTMLIFrameElement} */ (canvasElement.querySelector("iframe"));
    expect(iframe.getAttribute("src")).toBe("/login/ott?token=abc123");
    expect(iframe.style.display).toBe("none");
    expect(iframe.getAttribute("title")).toBe("Token authentication");

    // Form heading still visible
    expect(canvas.getByText("Sign in to continue")).toBeInTheDocument();

    // No error or logout cts-alert
    expect(canvasElement.querySelector("cts-alert")).toBeNull();
  },
};

export const ErrorAndLogout = {
  render: () => html`<cts-login-page error="Session expired" logout-message></cts-login-page>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Both error and logout cts-alerts are rendered.
    await waitFor(() => {
      expect(canvasElement.querySelector(".oidf-alert-danger")).toBeTruthy();
      expect(canvasElement.querySelector(".oidf-alert-info")).toBeTruthy();
    });

    // Error alert contains the error text
    const errorAlert = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".oidf-alert-danger")
    );
    expect(errorAlert.textContent).toContain("Session expired");
    expect(errorAlert.classList.contains("bg-danger")).toBe(false);

    // Logout message is also shown
    const logoutAlert = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".oidf-alert-info")
    );
    expect(logoutAlert.textContent).toContain("You have been logged out.");
    expect(logoutAlert.classList.contains("bg-info")).toBe(false);

    // OAuth buttons still render
    expect(canvas.getByText("Proceed with Google")).toBeInTheDocument();
    expect(canvas.getByText("Proceed with GitLab")).toBeInTheDocument();

    // Public links still present
    expect(canvas.getByText("View published logs")).toBeInTheDocument();
    expect(canvas.getByText("View published plans")).toBeInTheDocument();
  },
};
