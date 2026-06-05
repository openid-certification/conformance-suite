import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-form-field.js";

export default {
  title: "Components/cts-form-field",
  component: "cts-form-field",
};

export const TextInput = {
  render: () => html`
    <cts-form-field
      name="client.client_id"
      .schema=${{ type: "string", title: "Client ID", description: "OAuth 2.0 client identifier" }}
      value="my-client-123"
    ></cts-form-field>
  `,
  async play({ canvasElement, step }) {
    const label = canvasElement.querySelector(".oidf-label");
    const input = canvasElement.querySelector('input[type="text"]');
    await step("label renders with OIDF typography as a real <label>", async () => {
      expect(label).toBeTruthy();
      expect(label.textContent).toContain("Client ID");
      // OIDF label typography (bold / fs-12 / fg-soft) lives on .oidf-label directly,
      // sentence-cased — no longer borrowing the uppercase .t-overline utility.
      expect(getComputedStyle(label).textTransform).toBe("none");
      // The label must be a real <label> with `for` pointing at the input id, so
      // clicking the label focuses the field and screen readers announce the name.
      expect(label.tagName).toBe("LABEL");
    });
    await step("text input renders and is wired to its label", async () => {
      expect(input).toBeTruthy();
      expect(input.classList.contains("oidf-input")).toBe(true);
      expect(input.value).toBe("my-client-123");
      expect(input.id).toBeTruthy();
      expect(label.getAttribute("for")).toBe(input.id);
    });
    await step("help text renders and is linked via aria-describedby", async () => {
      const help = canvasElement.querySelector(".oidf-help");
      expect(help.textContent).toContain("OAuth 2.0 client identifier");
      expect(help.classList.contains("t-meta")).toBe(true);
      // aria-describedby links the help span back to the input.
      expect(input.getAttribute("aria-describedby") || "").toContain(help.id);
    });
  },
};

export const UrlInput = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value="https://accounts.example.com"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector('input[type="url"]');
    expect(input).toBeTruthy();
    expect(input.classList.contains("oidf-input")).toBe(true);
    expect(input.value).toBe("https://accounts.example.com");
  },
};

export const PasswordInput = {
  render: () => html`
    <cts-form-field
      name="client.client_secret"
      .schema=${{ type: "string", format: "password", title: "Client Secret" }}
      value="s3cret"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = canvasElement.querySelector('input[type="password"]');
    expect(input).toBeTruthy();
    expect(input.classList.contains("oidf-input")).toBe(true);
    expect(input.value).toBe("s3cret");
  },
};

export const JsonTextarea = {
  render: () => html`
    <cts-form-field
      name="client.jwks"
      .schema=${{ type: "object", format: "json", title: "Client JWKS" }}
      value='{"keys":[{"kty":"RSA"}]}'
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(textarea.classList.contains("oidf-textarea")).toBe(true);
    // JSON inputs render in monospace.
    expect(textarea.classList.contains("is-mono")).toBe(true);
  },
};

export const SelectDropdown = {
  render: () => html`
    <cts-form-field
      name="client.token_endpoint_auth_method"
      .schema=${{
        type: "string",
        title: "Auth Method",
        enum: ["client_secret_basic", "client_secret_post", "private_key_jwt"],
      }}
      value="client_secret_basic"
    ></cts-form-field>
  `,
  async play({ canvasElement, step }) {
    const select = canvasElement.querySelector("select.oidf-select");
    await step("select renders with enum options plus a placeholder", async () => {
      expect(select).toBeTruthy();
      const options = select.querySelectorAll("option");
      expect(options.length).toBe(4); // 3 enum + 1 placeholder
    });
    await step("label/select are wired for screen readers", async () => {
      // Label/select must be wired so screen readers announce "Auth Method" when
      // the dropdown is focused.
      const label = canvasElement.querySelector("label.oidf-label");
      expect(label).toBeTruthy();
      expect(select.id).toBeTruthy();
      expect(label.getAttribute("for")).toBe(select.id);
    });
    await step("line-height resolves to font-size to keep closed-state text centred", async () => {
      // line-height: 1 → resolved value matches font-size, preventing the
      // closed-state text from drifting inside the fixed 34px height across browsers.
      const cs = getComputedStyle(select);
      expect(cs.lineHeight).toBe(cs.fontSize);
    });
  },
};

export const BooleanCheckbox = {
  render: () => html`
    <cts-form-field
      name="client.use_mtls"
      .schema=${{ type: "boolean", title: "Use mTLS", description: "Enable mutual TLS" }}
      value="true"
    ></cts-form-field>
  `,
  async play({ canvasElement, step }) {
    const checkbox = canvasElement.querySelector('input[type="checkbox"]');
    await step("checkbox renders checked with the OIDF class", async () => {
      expect(checkbox).toBeTruthy();
      expect(checkbox.classList.contains("oidf-checkbox")).toBe(true);
      expect(checkbox.checked).toBe(true);
    });
    await step("checkbox label text is wired to the input", async () => {
      const checkLabel = canvasElement.querySelector(".oidf-checkbox-label");
      expect(checkLabel.textContent).toContain("Enable mutual TLS");
      // Clicking the label text toggles the checkbox — `for` must point at the
      // input id, otherwise the click target is silently broken.
      expect(checkbox.id).toBeTruthy();
      expect(checkLabel.getAttribute("for")).toBe(checkbox.id);
    });
  },
};

export const WithError = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value=""
      error="Required field"
    ></cts-form-field>
  `,
  async play({ canvasElement, step }) {
    const input = canvasElement.querySelector("input");
    await step("error state lands on the input control and its ARIA", async () => {
      // Error state lands on the rendered control via .is-error so the rust
      // border colour applies — no host-level class manipulation.
      expect(input.classList.contains("is-error")).toBe(true);
      // ARIA mirrors the visual error state for assistive tech.
      expect(input.getAttribute("aria-invalid")).toBe("true");
    });
    await step("error message renders as an alert linked back to the input", async () => {
      const error = canvasElement.querySelector(".oidf-error");
      expect(error).toBeTruthy();
      expect(error.textContent).toBe("Required field");
      expect(error.getAttribute("role")).toBe("alert");
      expect(error.id).toBeTruthy();
      expect(input.getAttribute("aria-describedby") || "").toContain(error.id);
    });
  },
};

/**
 * The original WithError only covered the text-input branch. JSON / select /
 * checkbox each render a different control and the `is-error` class has to
 * land on the right element — these stories pin that contract.
 */

export const WithErrorTextarea = {
  render: () => html`
    <cts-form-field
      name="client.jwks"
      .schema=${{ type: "object", format: "json", title: "Client JWKS" }}
      value="not json"
      error="Must be valid JSON"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(textarea.classList.contains("is-error")).toBe(true);
    // is-error must NOT land on a sibling or parent.
    expect(canvasElement.querySelector("input")).toBeNull();
    expect(canvasElement.querySelector(".oidf-error").textContent).toBe("Must be valid JSON");
  },
};

export const WithErrorSelect = {
  render: () => html`
    <cts-form-field
      name="client.token_endpoint_auth_method"
      .schema=${{
        type: "string",
        title: "Auth Method",
        enum: ["client_secret_basic", "private_key_jwt"],
      }}
      value=""
      error="Pick an auth method"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector("select.oidf-select");
    expect(select).toBeTruthy();
    expect(select.classList.contains("is-error")).toBe(true);
    expect(canvasElement.querySelector(".oidf-error").textContent).toBe("Pick an auth method");
  },
};

export const WithErrorCheckbox = {
  render: () => html`
    <cts-form-field
      name="client.use_mtls"
      .schema=${{ type: "boolean", title: "Use mTLS", description: "Enable mutual TLS" }}
      value="false"
      error="mTLS is required for this profile"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const checkbox = canvasElement.querySelector('input[type="checkbox"]');
    expect(checkbox).toBeTruthy();
    // The error message still renders next to the checkbox row — the visual
    // signal is the rust-coloured message, not a class on the checkbox itself.
    expect(canvasElement.querySelector(".oidf-error").textContent).toBe(
      "mTLS is required for this profile",
    );
  },
};

export const ChangeEvent = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", format: "uri", title: "Issuer URL" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    /** @type {any} */
    let receivedEvent = null;
    canvasElement.addEventListener("cts-field-change", (e) => {
      receivedEvent = /** @type {CustomEvent} */ (e).detail;
    });
    const input = canvasElement.querySelector("input");
    await userEvent.type(input, "https://example.com");
    expect(receivedEvent).toBeTruthy();
    expect(receivedEvent.field).toBe("server.issuer");
    expect(receivedEvent.value).toContain("https://example.com");
  },
};

export const Disabled = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", title: "Issuer URL" }}
      value="https://locked.example.com"
      disabled
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    expect(canvasElement.querySelector("input").disabled).toBe(true);
  },
};

/**
 * String-typed fields whose name's leaf segment ends in a PEM/JWKS/key
 * suffix (see `lib/config-field-types.js`) should render as `<textarea>`
 * so pasting multi-line credentials lands cleanly. The following stories
 * pin the per-suffix routing — including the regression edge case where
 * `_uri` shares the `jwks` substring but must stay a single-line URL.
 */

export const CertificateMultilineTextarea = {
  render: () => html`
    <cts-form-field
      name="client.certificate"
      .schema=${{ type: "string", title: "Certificate" }}
      value="-----BEGIN CERTIFICATE-----&#10;MIIB...&#10;-----END CERTIFICATE-----"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(textarea.classList.contains("oidf-textarea")).toBe(true);
    // Cert content is monospace-friendly; mirrors the JSON textarea convention.
    expect(textarea.classList.contains("is-mono")).toBe(true);
    // The catch-all single-line input branch must NOT also render.
    expect(canvasElement.querySelector("input")).toBeNull();
  },
};

export const PrivateKeyMultilineTextarea = {
  render: () => html`
    <cts-form-field
      name="client.private_key"
      .schema=${{ type: "string", title: "Private key" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(canvasElement.querySelector("input")).toBeNull();
  },
};

export const MtlsCertMultilineTextarea = {
  render: () => html`
    <cts-form-field
      name="mtls.cert"
      .schema=${{ type: "string", title: "mtls.cert" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    // The real catalog key is `mtls.cert` (4-letter suffix), not
    // `mtls.certificate` — the suffix list must include `cert` for the mtls
    // namespace to route through the textarea branch.
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(canvasElement.querySelector("input")).toBeNull();
  },
};

export const MtlsKeyMultilineTextarea = {
  render: () => html`
    <cts-form-field
      name="mtls.key"
      .schema=${{ type: "string", title: "mtls.key" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(canvasElement.querySelector("input")).toBeNull();
  },
};

export const TrustAnchorPemMultilineTextarea = {
  render: () => html`
    <cts-form-field
      name="vci.client_attestation_trust_anchor_pem"
      .schema=${{ type: "string", title: "Trust anchor PEM" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    // Suffix containment via the `_pem` tail. Confirms the matcher fires on
    // arbitrarily deep leaves, not just the simple `certificate` case.
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(canvasElement.querySelector("input")).toBeNull();
  },
};

export const CertificateChainMatchesViaSuffix = {
  render: () => html`
    <cts-form-field
      name="client.certificate_chain"
      .schema=${{ type: "string", title: "Certificate chain" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    // Edge case: the leaf contains `certificate` but ends with `_chain`. The
    // matcher's optional `_chain` tail is what carries this case — without it
    // the leaf would slip through to `<input>` and the field would be unusable.
    const textarea = canvasElement.querySelector("textarea");
    expect(textarea).toBeTruthy();
    expect(canvasElement.querySelector("input")).toBeNull();
  },
};

export const JwksUriStaysSingleLineUrlInput = {
  render: () => html`
    <cts-form-field
      name="server.jwks_uri"
      .schema=${{ type: "string", format: "uri", title: "JWKS URI" }}
      value="https://example.com/.well-known/jwks.json"
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    // Regression assertion: even though the leaf contains the `jwks` token,
    // its trailing `_uri` prevents the suffix anchor from matching, and the
    // `format: "uri"` check guards against a future regex slip. Either fence
    // alone is enough — together they make this surface bulletproof.
    const input = canvasElement.querySelector('input[type="url"]');
    expect(input).toBeTruthy();
    expect(canvasElement.querySelector("textarea")).toBeNull();
  },
};

/**
 * Publish dropdown affordance (U10 of MR 1998). The wire format keeps the
 * historical `["", "summary", "everything"]` so the backend
 * (`Strings.emptyToNull` in `TestRunner.java`) collapses `""` to null =
 * unpublished. The catalog adds an `enumLabels` parallel array that maps the
 * empty value to "No", so the dropdown reads like the pre-redesign UI rather
 * than rendering an unlabelled blank entry next to "Select...".
 */

export const PublishDropdownLabelsAndDefault = {
  render: () => html`
    <cts-form-field
      name="publish"
      .schema=${{
        type: "string",
        title: "publish",
        enum: ["", "summary", "everything"],
        enumLabels: ["No", "Summary", "Everything"],
      }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement, step }) {
    const select = /** @type {HTMLSelectElement} */ (
      canvasElement.querySelector("select.oidf-select")
    );
    const options = select.querySelectorAll("option");
    await step(
      "renders exactly the three enumLabel options without a duplicate placeholder",
      async () => {
        expect(select).toBeTruthy();
        // Three options exactly: No / Summary / Everything. The leading
        // <option value="">Select...</option> placeholder must be suppressed
        // because "" is already in the enum — rendering both would leave the
        // dropdown with two value="" entries.
        expect(options.length).toBe(3);
        expect(options[0].value).toBe("");
        expect(options[0].textContent).toBe("No");
        expect(options[1].value).toBe("summary");
        expect(options[1].textContent).toBe("Summary");
        expect(options[2].value).toBe("everything");
        expect(options[2].textContent).toBe("Everything");
      },
    );
    await step('"No" is the default selection so a fresh form submits unpublished', async () => {
      // "No" is the default selection so a fresh form submits an empty publish
      // value, which the backend treats as unpublished.
      expect(select.value).toBe("");
      expect(options[0].selected).toBe(true);
    });
  },
};

export const PublishDropdownChangeEmitsWireValue = {
  render: () => html`
    <cts-form-field
      name="publish"
      .schema=${{
        type: "string",
        title: "publish",
        enum: ["", "summary", "everything"],
        enumLabels: ["No", "Summary", "Everything"],
      }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    /** @type {any} */
    let received = null;
    canvasElement.addEventListener("cts-field-change", (e) => {
      received = /** @type {CustomEvent} */ (e).detail;
    });
    const select = /** @type {HTMLSelectElement} */ (
      canvasElement.querySelector("select.oidf-select")
    );
    // Picking "Everything" must emit the WIRE value ("everything"), not the
    // human label, otherwise the backend would receive a string it does not
    // recognise as a publish level.
    await userEvent.selectOptions(select, "everything");
    expect(received).toBeTruthy();
    expect(received.field).toBe("publish");
    expect(received.value).toBe("everything");
  },
};

/**
 * Focus state must be keyboard-visible: the input picks up the OIDF orange
 * border + focus ring (`--focus-ring`) the moment it gains focus. We assert
 * via the computed style after focusing the rendered control.
 */
export const FocusState = {
  render: () => html`
    <cts-form-field
      name="server.issuer"
      .schema=${{ type: "string", title: "Issuer URL" }}
      value=""
    ></cts-form-field>
  `,
  async play({ canvasElement }) {
    const input = /** @type {HTMLInputElement} */ (canvasElement.querySelector("input"));
    input.focus();
    expect(document.activeElement).toBe(input);
    const computed = getComputedStyle(input);
    // --orange-400 = #EB8B35 → rgb(235, 139, 53)
    expect(computed.borderTopColor).toBe("rgb(235, 139, 53)");
    // --focus-ring expands to a 3px box-shadow; assert it is non-empty.
    expect(computed.boxShadow).not.toBe("none");
  },
};
