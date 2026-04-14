import { html } from "lit";
import { expect, within, waitFor, userEvent, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import "./cts-spec-cascade.js";

const MOCK_PLANS = [
  {
    planName: "oidcc-basic-certification-test-plan",
    displayName: "OpenID Connect Core: Basic Certification Profile",
    specFamily: "OIDCC",
    specVersion: "Final",
    entityUnderTest: "OP",
    modules: [{ testModule: "oidcc-server" }],
  },
  {
    planName: "oidcc-implicit-certification-test-plan",
    displayName: "OpenID Connect Core: Implicit Certification Profile",
    specFamily: "OIDCC",
    specVersion: "Final",
    entityUnderTest: "OP",
    modules: [{ testModule: "oidcc-server-implicit" }],
  },
  {
    planName: "oidcc-client-basic-certification-test-plan",
    displayName: "OpenID Connect Client: Basic Certification",
    specFamily: "OIDCC",
    specVersion: "Final",
    entityUnderTest: "RP",
    modules: [{ testModule: "oidcc-client-test" }],
  },
  {
    planName: "fapi2-security-profile-final-test-plan",
    displayName: "FAPI 2.0 Security Profile",
    specFamily: "FAPI",
    specVersion: "Final",
    entityUnderTest: "OP",
    modules: [{ testModule: "fapi2-security-profile-happy-flow" }],
  },
  {
    planName: "fapi-ciba-test-plan",
    displayName: "FAPI-CIBA: Client Initiated Backchannel Authentication",
    specFamily: "FAPI-CIBA",
    specVersion: "Final",
    entityUnderTest: "OP",
    modules: [{ testModule: "fapi-ciba-happy-flow" }],
  },
  {
    planName: "ssf-transmitter-test-plan",
    displayName: "Shared Signals Framework: Transmitter",
    specFamily: "SSF",
    specVersion: "Draft",
    entityUnderTest: "Transmitter",
    modules: [{ testModule: "ssf-transmitter-happy-flow" }],
  },
];

export default {
  title: "Components/cts-spec-cascade",
  component: "cts-spec-cascade",
};

export const WithProvidedPlans = {
  render: () => html`<cts-spec-cascade .plans=${MOCK_PLANS}></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const familySelect = canvas.getByLabelText("Specification");
    expect(familySelect).toBeInTheDocument();
  },
};
