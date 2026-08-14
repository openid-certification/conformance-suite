import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import "./cts-login-page.js";

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

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("form heading is present", async () => {
      await waitFor(() => {
        expect(canvas.getByText("Sign in to continue")).toBeInTheDocument();
      });
    });

    await step("two-pane shell renders on the muted background", async () => {
      const wrapper = canvasElement.querySelector(".oidf-login-page");
      expect(wrapper).toBeTruthy();
      const card = canvasElement.querySelector(".oidf-login-card");
      expect(card).toBeTruthy();
    });

    await step("brand band renders the OIDF wordmark and capability pillars", async () => {
      const brand = canvasElement.querySelector(".oidf-login-brand");
      expect(brand).toBeTruthy();
      const brandLogo = /** @type {HTMLImageElement} */ (
        canvasElement.querySelector(".oidf-login-brand__logo")
      );
      expect(brandLogo).toBeTruthy();
      expect(brandLogo.getAttribute("src")).toBe("/images/openid-dark.svg");
      expect(brandLogo.getAttribute("alt")).toBe("OpenID Foundation");
      expect(canvasElement.querySelectorAll(".oidf-login-brand__pillars li").length).toBe(4);
    });

    await step("sign-in button is the single IdP entry point", async () => {
      const idpAnchor = /** @type {HTMLAnchorElement} */ (
        canvas.getByText("Sign in with OpenID").closest("a")
      );
      expect(idpAnchor).toBeTruthy();
      expect(idpAnchor.getAttribute("href")).toBe("/oauth2/authorization/idp");

      // The IdP brokers the upstream providers, so the retired per-provider
      // buttons must not come back alongside it.
      expect(canvasElement.querySelector('a[href="/oauth2/authorization/google"]')).toBeNull();
      expect(canvasElement.querySelector('a[href="/oauth2/authorization/gitlab"]')).toBeNull();
    });

    await step("sign-in button carries the design-system button treatment", async () => {
      const idpAnchor = /** @type {HTMLAnchorElement} */ (
        canvas.getByText("Sign in with OpenID").closest("a")
      );
      expect(idpAnchor.classList.contains("oidf-btn")).toBe(true);
      expect(idpAnchor.classList.contains("oidf-btn-secondary")).toBe(true);
      // oidf-btn-secondary is styled around a leading icon; without one the
      // button reads as under-filled next to the rest of the design system.
      expect(idpAnchor.querySelector("cts-icon")?.getAttribute("name")).toBe("user-01");
      // Bootstrap btn-* must NOT leak through — the legacy page styled this
      // anchor with btn/btn-lg/btn-primary.
      expect([...idpAnchor.classList].some((c) => /^btn(-|$)/.test(c))).toBe(false);
      // This is a plain <a>, not a mounted cts-link-button, so it depends on the
      // shared <style id="cts-button-styles"> block that cts-link-button.js
      // injects on import. Without it the anchor renders as a plain text link
      // instead of a 44px lg button. (Prior regression: that injection used to
      // happen in connectedCallback, which this page never triggers.)
      expect(document.getElementById("cts-button-styles")).toBeTruthy();
      expect(getComputedStyle(idpAnchor).height).toBe("44px");
    });

    await step("public links have correct hrefs and live in rich-list anchors", async () => {
      expect(canvas.getByText("View published logs")).toBeInTheDocument();
      expect(canvas.getByText("View published plans")).toBeInTheDocument();

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
    });

    await step("no alert, iframe, or Bootstrap layout classes render by default", async () => {
      // No error or logout cts-alert renders by default
      expect(canvasElement.querySelector("cts-alert")).toBeNull();

      // No hidden iframe
      const iframe = canvasElement.querySelector("iframe");
      expect(iframe).toBeNull();

      // Bootstrap layout classes must NOT leak through.
      expect(canvasElement.querySelector(".container-fluid")).toBeNull();
      expect(canvasElement.querySelector(".row")).toBeNull();
    });
  },
};

export const WithError = {
  render: () => html`<cts-login-page error="Invalid credentials"></cts-login-page>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("error alert is visible with role=alert and the error text", async () => {
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
    });

    await step("no logout alert renders", async () => {
      expect(canvasElement.querySelector(".oidf-alert-info")).toBeNull();
    });

    await step("IdP sign-in button still renders", async () => {
      expect(canvas.getByText("Sign in with OpenID")).toBeInTheDocument();
    });
  },
};

export const PostLogout = {
  render: () => html`<cts-login-page logout-message></cts-login-page>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("logout message renders inside an info-variant cts-alert", async () => {
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
    });

    await step("no error alert renders", async () => {
      expect(canvasElement.querySelector(".oidf-alert-danger")).toBeNull();
    });

    await step("IdP sign-in button still renders", async () => {
      expect(canvas.getByText("Sign in with OpenID")).toBeInTheDocument();
    });
  },
};

export const TokenAuth = {
  render: () => html`<cts-login-page token-auth-url="/login/ott?token=abc123"></cts-login-page>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("hidden iframe is present for OTT auth", async () => {
      await waitFor(() => {
        const iframe = canvasElement.querySelector("iframe");
        expect(iframe).toBeTruthy();
      });

      const iframe = /** @type {HTMLIFrameElement} */ (canvasElement.querySelector("iframe"));
      expect(iframe.getAttribute("src")).toBe("/login/ott?token=abc123");
      expect(iframe.style.display).toBe("none");
      expect(iframe.getAttribute("title")).toBe("Token authentication");
    });

    await step("form heading still visible", async () => {
      expect(canvas.getByText("Sign in to continue")).toBeInTheDocument();
    });

    await step("no error or logout cts-alert renders", async () => {
      expect(canvasElement.querySelector("cts-alert")).toBeNull();
    });
  },
};

export const ErrorAndLogout = {
  render: () => html`<cts-login-page error="Session expired" logout-message></cts-login-page>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("both error and logout cts-alerts are rendered", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector(".oidf-alert-danger")).toBeTruthy();
        expect(canvasElement.querySelector(".oidf-alert-info")).toBeTruthy();
      });
    });

    await step("error alert contains the error text", async () => {
      const errorAlert = /** @type {HTMLElement} */ (
        canvasElement.querySelector(".oidf-alert-danger")
      );
      expect(errorAlert.textContent).toContain("Session expired");
      expect(errorAlert.classList.contains("bg-danger")).toBe(false);
    });

    await step("logout message is also shown", async () => {
      const logoutAlert = /** @type {HTMLElement} */ (
        canvasElement.querySelector(".oidf-alert-info")
      );
      expect(logoutAlert.textContent).toContain("You have been logged out.");
      expect(logoutAlert.classList.contains("bg-info")).toBe(false);
    });

    await step("IdP sign-in button still renders", async () => {
      expect(canvas.getByText("Sign in with OpenID")).toBeInTheDocument();
    });

    await step("public links still present", async () => {
      expect(canvas.getByText("View published logs")).toBeInTheDocument();
      expect(canvas.getByText("View published plans")).toBeInTheDocument();
    });
  },
};
