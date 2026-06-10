/**
 * THEMING SPIKE — prototype demo bar (throwaway scaffolding, NOT a cts-* component).
 *
 * A thin 11px-mono strip injected at the very top of every page so MR reviewers
 * can drive the prototype without reading docs: switch between the bundled demo
 * themes (or back to OIDF default), jump to the theme admin page, and apply the
 * active theme's pre-baked presets. Delete this file (and its <script> tags)
 * when the spike is done.
 *
 * Theme switching POSTs the bundled definitions to /api/theme, so it needs a
 * signed-in session (the spike API is open to all signed-in users) and a
 * database-managed theme (a fintechlabs.theme.dir deployment reports 409 —
 * the bar says so instead of switching).
 */

import { getTheme } from "./theme-client.js";

const DEMO_LOGOS = {
  helseid: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 232 56" role="img" aria-label="HelseID"><rect x="2" y="2" width="52" height="52" rx="12" fill="#FFFFFF"/><path d="M22 14h12v8h8v12h-8v8H22v-8h-8V22h8z" fill="#0067C5"/><text x="66" y="39" font-family="system-ui, sans-serif" font-size="30" font-weight="700" fill="#FFFFFF">Helse<tspan font-weight="400">ID</tspan></text></svg>`,
  verde: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="58 32 296 64" role="img" aria-label="Verde Open Finance"><g transform="translate(58 32)"><circle cx="28" cy="28" r="26" fill="none" stroke="#FFFFFF" stroke-width="4"/><path d="M28 44c0-14 6-22 16-26-2 12-6 22-16 26z" fill="#7FD1A2"/><path d="M28 44c0-14-6-22-16-26 2 12 6 22 16 26z" fill="#FFFFFF"/><text x="66" y="38" font-family="system-ui, sans-serif" font-size="34" font-weight="700" fill="#FFFFFF">verde<tspan fill="#7FD1A2">.</tspan></text></g></svg>`,
  lumina: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 56" role="img" aria-label="Lumina Trust"><path d="M28 4l6 18 18 6-18 6-6 18-6-18-18-6 18-6z" fill="#FFFFFF"/><path d="M28 16l3 9 9 3-9 3-3 9-3-9-9-3 9-3z" fill="#C9B8F0"/><text x="62" y="39" font-family="system-ui, sans-serif" font-size="30" font-weight="700" fill="#FFFFFF">Lumina<tspan font-weight="400" fill="#C9B8F0"> Trust</tspan></text></svg>`,
};

/** @param {string} key @returns {string} base64 data URL for the demo logo */
function logoDataUrl(key) {
  return "data:image/svg+xml;base64," + btoa(DEMO_LOGOS[key]);
}

const DEMO_THEMES = [
  {
    key: "helseid",
    label: "helseid",
    theme: {
      version: 1,
      partner: { name: "HelseID" },
      brand: { accent: "#0067C5", logo: { data: null, alt: "HelseID", plate: false } },
      presets: [
        {
          id: "helseid-rp-basic",
          label: "HelseID RP certification — Basic profile",
          description:
            "Certify your relying party (client) against the HelseID profile. Plan, variants and the demo client are pre-configured.",
          planName: "oidcc-client-basic-certification-test-plan",
          variant: { client_registration: "static_client", request_type: "plain_http_request" },
          configuration: {
            alias: "helseid-rp",
            description: "HelseID RP certification (pre-baked demo configuration)",
            client: {
              client_id: "helseid-demo-rp",
              client_secret: "hid-demo-secret-not-for-production",
            },
          },
        },
      ],
    },
  },
  {
    key: "verde",
    label: "verde",
    theme: {
      version: 1,
      partner: { name: "Verde Open Finance" },
      brand: { accent: "#1E7A46", logo: { data: null, alt: "Verde Open Finance", plate: false } },
      presets: [
        {
          id: "verde-fapi2-op",
          label: "Verde ecosystem — FAPI 2 OP certification",
          description:
            "Certify your authorization server for the Verde Open Finance ecosystem with the FAPI 2 Security Profile.",
          planName: "fapi2-security-profile-final-test-plan",
          variant: {},
          configuration: {
            alias: "verde-op",
            description: "Verde Open Finance OP certification (pre-baked demo configuration)",
          },
        },
      ],
    },
  },
  {
    key: "lumina",
    label: "lumina",
    theme: {
      version: 1,
      partner: { name: "Lumina Trust" },
      brand: { accent: "#5B3FA8", logo: { data: null, alt: "Lumina Trust", plate: false } },
      presets: [
        {
          id: "lumina-vp-wallet",
          label: "Lumina wallet certification — OpenID4VP",
          description:
            "Certify your wallet for the Lumina Trust framework with the Verifiable Presentations test plan.",
          planName: "oid4vp-1final-wallet-test-plan",
          variant: {},
          configuration: {
            alias: "lumina-wallet",
            description: "Lumina Trust wallet certification (pre-baked demo configuration)",
          },
        },
      ],
    },
  },
];

const BAR_ID = "theming-spike-bar";

const BAR_CSS = `
#${BAR_ID} {
  display: flex; flex-wrap: wrap; align-items: center; gap: 2px 10px;
  padding: 4px 12px;
  background: #0d0b09;
  border-bottom: 1px solid #2e2a24;
  color: #8f877b;
  font-family: var(--font-mono, ui-monospace, monospace);
  font-size: 11px; line-height: 16px;
}
#${BAR_ID} .sb-tag { color: #5d564c; user-select: none; }
#${BAR_ID} .sb-tag::before { content: "▚ "; color: #3d382f; }
#${BAR_ID} .sb-sep { color: #3d382f; user-select: none; }
#${BAR_ID} button, #${BAR_ID} a {
  font: inherit; color: #b5ac9e; background: none; border: 0; padding: 0 2px;
  cursor: pointer; text-decoration: none; border-radius: 2px;
}
#${BAR_ID} button:hover, #${BAR_ID} a:hover { color: #fff; text-decoration: underline; }
#${BAR_ID} button:focus-visible, #${BAR_ID} a:focus-visible { outline: 1px solid #b5ac9e; outline-offset: 1px; }
#${BAR_ID} button[data-active="true"] { color: #ffd479; }
#${BAR_ID} button[data-active="true"]::before { content: "["; }
#${BAR_ID} button[data-active="true"]::after { content: "]"; }
#${BAR_ID} button[disabled] { color: #5d564c; cursor: default; text-decoration: none; }
#${BAR_ID} .sb-msg { color: #d8a25a; }
#${BAR_ID} .sb-right { margin-left: auto; display: flex; gap: 10px; align-items: center; }
`;

/** @returns {Promise<{source: string, theme?: object}>} active theme envelope */
async function fetchThemeEnvelope() {
  const theme = await getTheme(); // shared, memoized page-wide fetch
  return theme ? { source: theme.source, theme } : { source: "none" };
}

/**
 * Best-effort signed-in check from cts-navbar's sessionStorage cache — no
 * network (a probe here would re-introduce the /api/currentuser 401 noise the
 * login page deliberately avoids, and would trip the e2e fail-fast mocks).
 * Absent cache usually means signed out; worst case a signed-in first-time
 * visitor sees the hint until their next navigation.
 * @returns {boolean} whether a signed-in session is likely present
 */
function likelySignedIn() {
  try {
    return !!sessionStorage.getItem("cts-navbar:user");
  } catch {
    return false;
  }
}

/** @param {string} text @returns {HTMLSpanElement} */
function sep(text = "│") {
  const span = document.createElement("span");
  span.className = "sb-sep";
  span.textContent = text;
  return span;
}

/**
 * @param {object} envelope - /api/theme response
 * @returns {string|null} demo-theme key matching the active theme, "default" when unthemed, null when custom
 */
function activeKey(envelope) {
  if (!envelope || envelope.source === "none" || !envelope.theme) return "default";
  const name = envelope.theme.partner && envelope.theme.partner.name;
  const match = DEMO_THEMES.find((d) => d.theme.partner.name === name);
  return match ? match.key : null;
}

/** @param {object} envelope @param {HTMLElement} bar */
function render(envelope, bar) {
  bar.textContent = "";
  const active = activeKey(envelope);
  const fileManaged = envelope.source === "file";

  const tag = document.createElement("span");
  tag.className = "sb-tag";
  tag.textContent = "theming-spike";
  bar.append(tag, sep());

  const themeLabel = document.createElement("span");
  themeLabel.textContent = "theme:";
  bar.append(themeLabel);

  const options = [{ key: "default", label: "oidf-default", theme: null }, ...DEMO_THEMES];
  for (const option of options) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = option.label;
    button.dataset.active = String(active === option.key);
    button.disabled = fileManaged;
    button.title = fileManaged
      ? "Theme is managed by fintechlabs.theme.dir — switching disabled"
      : option.theme
        ? `Apply the ${option.theme.partner.name} demo theme`
        : "Remove the partner theme (OIDF branding)";
    button.addEventListener("click", () => applyTheme(option, bar));
    bar.append(button);
  }

  if (!fileManaged && !likelySignedIn()) {
    const hint = document.createElement("span");
    hint.className = "sb-msg";
    hint.textContent = "(sign in to switch)";
    hint.title = "Theme switching writes to /api/theme, which needs a signed-in session";
    bar.append(hint);
  }

  const presets =
    (envelope.theme && Array.isArray(envelope.theme.presets) && envelope.theme.presets) || [];
  if (presets.length > 0) {
    bar.append(sep());
    const presetsLabel = document.createElement("span");
    presetsLabel.textContent = "presets:";
    bar.append(presetsLabel);
    for (const preset of presets) {
      if (!preset || !preset.id) continue;
      const link = document.createElement("a");
      link.href = "schedule-test.html?preset=" + encodeURIComponent(preset.id);
      link.textContent = preset.id;
      link.title = (preset.label || preset.id) + " — opens schedule-test pre-filled";
      bar.append(link);
    }
  }

  const right = document.createElement("span");
  right.className = "sb-right";
  if (fileManaged) {
    const msg = document.createElement("span");
    msg.className = "sb-msg";
    msg.textContent = "config-as-code (fintechlabs.theme.dir) — switcher off";
    right.append(msg);
  }
  const admin = document.createElement("a");
  admin.href = "theme-admin.html";
  admin.textContent = "theme-admin →";
  right.append(admin);
  bar.append(right);
}

/** @param {{key: string, theme: object|null}} option @param {HTMLElement} bar */
async function applyTheme(option, bar) {
  const msg = document.createElement("span");
  msg.className = "sb-msg";
  msg.textContent = "applying…";
  bar.append(msg);
  try {
    let response;
    if (option.theme === null) {
      response = await fetch("/api/theme", { method: "DELETE" });
    } else {
      const body = structuredClone(option.theme);
      body.brand.logo.data = logoDataUrl(option.key);
      response = await fetch("/api/theme", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
    }
    if (response.ok) {
      try {
        sessionStorage.removeItem("cts-theme");
      } catch {
        /* best-effort */
      }
      window.location.reload();
      return;
    }
    msg.textContent =
      response.status === 401 || response.status === 403
        ? "sign in first (Google/GitLab) — theme switching writes to the server"
        : response.status === 409
          ? "config-as-code deployment — switching disabled"
          : `switch failed (${response.status})`;
  } catch {
    msg.textContent = "switch failed (network)";
  }
}

const style = document.createElement("style");
style.textContent = BAR_CSS;
document.head.appendChild(style);

const bar = document.createElement("div");
bar.id = BAR_ID;
bar.setAttribute("role", "region");
bar.setAttribute("aria-label", "Theming spike prototype controls");

fetchThemeEnvelope().then((envelope) => {
  render(envelope, bar);
  // Insert after the skip link when present so keyboard order keeps
  // "skip to main content" first.
  const skip = document.querySelector(".oidf-skip-link");
  if (skip && skip.parentElement === document.body) {
    skip.insertAdjacentElement("afterend", bar);
  } else {
    document.body.prepend(bar);
  }
});

export {};
