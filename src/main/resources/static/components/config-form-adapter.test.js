import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  ALWAYS_ON_FIELDS,
  buildConfigFormSchema,
  computeApplicableFields,
  computeExplicitHides,
  computeHiddenFields,
  setAtPath,
} from "./config-form-adapter.js";

/**
 * Small two-section catalog used by the focused tests. Real production uses
 * the full vendored config-field-catalog.json; the test fixture only exists
 * to make the adapter contracts explicit.
 */
const CATALOG = {
  sections: [
    {
      key: "_root",
      title: "Test Information",
      fields: [
        { key: "alias", label: "alias", type: "string", tooltip: "Used to ..." },
        { key: "description", label: "description", type: "string" },
        {
          key: "publish",
          label: "publish",
          type: "string",
          enum: ["", "summary", "everything"],
        },
      ],
    },
    {
      key: "client",
      title: "Client",
      fields: [
        { key: "client.client_id", label: "client_id", type: "string", tooltip: "OAuth id" },
        {
          key: "client.client_secret",
          label: "client_secret",
          type: "string",
          format: "password",
        },
        {
          key: "client.jwks",
          label: "client_jwks",
          type: "object",
          placeholder: "REQUIRED",
        },
      ],
    },
    {
      key: "federation_trust_anchor",
      title: "Test suite trust anchor",
      fields: [
        {
          key: "federation_trust_anchor.immediate_subordinates",
          label: "immediate_subordinates",
          type: "array",
          format: "newline-array",
        },
        {
          key: "federation_trust_anchor.trust_anchor_jwks",
          label: "trust_anchor_jwks",
          type: "string",
          required: true,
          jwksGenerator: "trust-anchor-jwks",
        },
      ],
    },
  ],
};

describe("computeApplicableFields", () => {
  it("returns only always-on fields when planInfo is null", () => {
    expect([...computeApplicableFields(null)]).toEqual(["alias", "description", "publish"]);
  });

  it("unions plan + module + selected variant configurationFields", () => {
    const plan = {
      configurationFields: ["alias", "client.client_id"],
      modules: [{ configurationFields: ["server.discoveryUrl"] }],
      variants: {
        client_auth: {
          variantValues: {
            mtls: { configurationFields: ["mtls.cert"] },
            client_secret_basic: { configurationFields: ["client.client_secret"] },
          },
        },
      },
    };
    const result = computeApplicableFields(plan, { client_auth: "mtls" });
    expect(result.has("alias")).toBe(true);
    expect(result.has("client.client_id")).toBe(true);
    expect(result.has("server.discoveryUrl")).toBe(true);
    expect(result.has("mtls.cert")).toBe(true);
    // The other variant value's field is not pulled in.
    expect(result.has("client.client_secret")).toBe(false);
    // Always-on fields are always present.
    for (const f of ALWAYS_ON_FIELDS) expect(result.has(f)).toBe(true);
  });

  it("ignores variant values of 'select' or empty (user hasn't picked yet)", () => {
    const plan = {
      configurationFields: ["alias"],
      variants: {
        client_auth: {
          variantValues: { mtls: { configurationFields: ["mtls.cert"] } },
        },
      },
    };
    const noPick = computeApplicableFields(plan, { client_auth: "select" });
    expect(noPick.has("mtls.cert")).toBe(false);
    const blank = computeApplicableFields(plan, { client_auth: "" });
    expect(blank.has("mtls.cert")).toBe(false);
  });
});

describe("computeExplicitHides", () => {
  it("returns empty set for null planInfo", () => {
    expect(computeExplicitHides(null).size).toBe(0);
  });

  it("unions plan + selected-variant hidesConfigurationFields", () => {
    const plan = {
      hidesConfigurationFields: ["client.client_secret"],
      variants: {
        client_auth: {
          variantValues: {
            mtls: { hidesConfigurationFields: ["client.jwks"] },
          },
        },
      },
    };
    const result = computeExplicitHides(plan, { client_auth: "mtls" });
    expect(result.has("client.client_secret")).toBe(true);
    expect(result.has("client.jwks")).toBe(true);
  });

  it("excludes always-on fields even if the variant tries to hide them", () => {
    const plan = {
      hidesConfigurationFields: ["alias", "publish"],
    };
    const result = computeExplicitHides(plan, {});
    expect(result.has("alias")).toBe(false);
    expect(result.has("publish")).toBe(false);
  });
});

describe("buildConfigFormSchema — variant pass-through (regression)", () => {
  // Plan-level configurationFields are empty; the catalog field is contributed
  // ONLY by the selected variant value. The schema MUST include it once the
  // variant is selected, otherwise the form renders an empty Client section
  // for any plan whose required fields come from variants (federation, FAPI2,
  // CIBA).
  const variantPlan = {
    configurationFields: ["alias"],
    variants: {
      client_auth: {
        variantValues: {
          mtls: { configurationFields: ["client.client_id"] },
          client_secret_basic: { configurationFields: ["client.client_secret"] },
        },
      },
    },
  };

  it("includes variant-contributed fields in the schema when selectedVariant is passed", () => {
    const { schema, uiSchema } = buildConfigFormSchema(variantPlan, CATALOG, {
      client_auth: "mtls",
    });
    expect(schema.properties["client.client_id"]).toBeTruthy();
    const clientSection = uiSchema.sections.find((s) => s.key === "client");
    expect(clientSection).toBeTruthy();
    expect(clientSection.fields).toContain("client.client_id");
  });

  it("omits variant-contributed fields when no variant is selected", () => {
    const { schema, uiSchema } = buildConfigFormSchema(variantPlan, CATALOG);
    expect(schema.properties["client.client_id"]).toBeUndefined();
    expect(uiSchema.sections.find((s) => s.key === "client")).toBeUndefined();
  });

  it("includes only the selected variant's contributions, not all values", () => {
    const { schema } = buildConfigFormSchema(variantPlan, CATALOG, { client_auth: "mtls" });
    expect(schema.properties["client.client_id"]).toBeTruthy();
    expect(schema.properties["client.client_secret"]).toBeUndefined();
  });
});

describe("setAtPath", () => {
  it("creates nested intermediate objects and writes the leaf", () => {
    const obj = {};
    // Use require-style runtime import via dynamic import is overkill; the
    // function is exported at the top of the test file.
    setAtPath(obj, "federation.op_ec_jwks", { keys: [{ kty: "RSA" }] });
    expect(obj).toEqual({ federation: { op_ec_jwks: { keys: [{ kty: "RSA" }] } } });
  });

  it("preserves sibling keys at each level", () => {
    const obj = { federation: { rp_ec_jwks: { existing: true } } };
    setAtPath(obj, "federation.op_ec_jwks", { added: true });
    expect(obj.federation.rp_ec_jwks).toEqual({ existing: true });
    expect(obj.federation.op_ec_jwks).toEqual({ added: true });
  });

  it("overwrites a scalar leaf with an object", () => {
    const obj = { client: { jwks: "stringified" } };
    setAtPath(obj, "client.jwks", { keys: [] });
    expect(obj.client.jwks).toEqual({ keys: [] });
  });

  it("handles a single-segment path", () => {
    const obj = {};
    setAtPath(obj, "alias", "my-alias");
    expect(obj).toEqual({ alias: "my-alias" });
  });
});

describe("buildConfigFormSchema", () => {
  it("emits flat properties keyed by full path with sections holding explicit field lists", () => {
    const plan = {
      configurationFields: ["alias", "client.client_id"],
    };
    const { schema, uiSchema } = buildConfigFormSchema(plan, CATALOG);
    // Always-on fields are in applicable, so all _root fields render.
    expect(Object.keys(schema.properties).sort()).toEqual([
      "alias",
      "client.client_id",
      "description",
      "publish",
    ]);
    expect(schema.properties.alias).toEqual({
      type: "string",
      title: "alias",
      description: "Used to ...",
    });
    expect(schema.properties["client.client_id"]).toEqual({
      type: "string",
      title: "client_id",
      description: "OAuth id",
    });
    expect(uiSchema.sections).toEqual([
      { key: "_root", title: "Test Information", fields: ["alias", "description", "publish"] },
      { key: "client", title: "Client", fields: ["client.client_id"] },
    ]);
  });

  it("omits sections whose catalog fields are all outside the applicable set", () => {
    const plan = { configurationFields: ["alias"] };
    const { uiSchema } = buildConfigFormSchema(plan, CATALOG);
    const sectionKeys = uiSchema.sections.map((s) => s.key);
    expect(sectionKeys).toContain("_root");
    expect(sectionKeys).not.toContain("client");
    expect(sectionKeys).not.toContain("federation_trust_anchor");
  });

  it("passes through catalog metadata as x-cts-* annotations", () => {
    const plan = {
      configurationFields: [
        "client.client_secret",
        "client.jwks",
        "federation_trust_anchor.trust_anchor_jwks",
        "federation_trust_anchor.immediate_subordinates",
      ],
    };
    const { schema } = buildConfigFormSchema(plan, CATALOG);
    expect(schema.properties["client.client_secret"].format).toBe("password");
    expect(schema.properties["client.jwks"]["x-cts-placeholder"]).toBe("REQUIRED");
    expect(schema.properties["federation_trust_anchor.trust_anchor_jwks"]["x-cts-required"]).toBe(
      true,
    );
    expect(
      schema.properties["federation_trust_anchor.trust_anchor_jwks"]["x-cts-jwks-generator"],
    ).toBe("trust-anchor-jwks");
    expect(schema.properties["federation_trust_anchor.immediate_subordinates"].format).toBe(
      "newline-array",
    );
    expect(schema.properties.publish.enum).toEqual(["", "summary", "everything"]);
  });

  describe("unknown backend fields", () => {
    let warnSpy;
    beforeEach(() => {
      warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    });
    afterEach(() => {
      warnSpy.mockRestore();
    });

    it("surfaces backend-known fields missing from the catalog in an 'Other' section and warns", () => {
      const plan = {
        configurationFields: ["alias", "unknown.new_field", "another.brand_new"],
      };
      const { schema, uiSchema } = buildConfigFormSchema(plan, CATALOG);
      expect(schema.properties["unknown.new_field"]).toEqual({
        type: "string",
        title: "unknown.new_field",
      });
      expect(schema.properties["another.brand_new"]).toEqual({
        type: "string",
        title: "another.brand_new",
      });
      const otherSection = uiSchema.sections.find((s) => s.key === "_orphans");
      expect(otherSection).toBeTruthy();
      expect(otherSection.title).toBe("Other");
      expect(otherSection.fields.sort()).toEqual(["another.brand_new", "unknown.new_field"]);
      expect(warnSpy).toHaveBeenCalledOnce();
      expect(warnSpy.mock.calls[0][0]).toMatch(/2 configurationFields not in catalog/);
    });
  });
});

describe("computeHiddenFields", () => {
  it("hides every catalog field outside the applicable set", () => {
    const plan = { configurationFields: ["client.client_id"] };
    const hidden = computeHiddenFields(plan, {}, CATALOG);
    expect(hidden.has("client.client_secret")).toBe(true);
    expect(hidden.has("client.jwks")).toBe(true);
    expect(hidden.has("federation_trust_anchor.immediate_subordinates")).toBe(true);
    expect(hidden.has("federation_trust_anchor.trust_anchor_jwks")).toBe(true);
    expect(hidden.has("client.client_id")).toBe(false);
  });

  it("hides explicit `hidesConfigurationFields` entries even when applicable", () => {
    const plan = {
      configurationFields: ["client.client_id", "client.client_secret"],
      hidesConfigurationFields: ["client.client_secret"],
    };
    const hidden = computeHiddenFields(plan, {}, CATALOG);
    expect(hidden.has("client.client_id")).toBe(false);
    expect(hidden.has("client.client_secret")).toBe(true);
  });

  it("never hides always-on fields", () => {
    const plan = {
      hidesConfigurationFields: ["alias", "description", "publish"],
    };
    const hidden = computeHiddenFields(plan, {}, CATALOG);
    for (const f of ALWAYS_ON_FIELDS) expect(hidden.has(f)).toBe(false);
  });

  it("respects variant-level hides", () => {
    const plan = {
      configurationFields: ["client.client_id", "client.client_secret"],
      variants: {
        client_auth: {
          variantValues: {
            mtls: { hidesConfigurationFields: ["client.client_secret"] },
          },
        },
      },
    };
    const hiddenMtls = computeHiddenFields(plan, { client_auth: "mtls" }, CATALOG);
    expect(hiddenMtls.has("client.client_secret")).toBe(true);
    const hiddenOther = computeHiddenFields(plan, { client_auth: "select" }, CATALOG);
    expect(hiddenOther.has("client.client_secret")).toBe(false);
  });

  it("treats catalog-unknown applicable fields as visible (handled by 'Other' section)", () => {
    const plan = { configurationFields: ["alias", "unknown.new_field"] };
    const hidden = computeHiddenFields(plan, {}, CATALOG);
    expect(hidden.has("unknown.new_field")).toBe(false);
  });
});

describe("integration: full federation flow", () => {
  it("hides federation_trust_anchor.* fields when the federation variant is OP-only", () => {
    const plan = {
      configurationFields: ["alias", "federation.entity_identifier"],
      variants: {
        federation_role: {
          variantValues: {
            op: {
              configurationFields: ["federation.op_ec_jwks"],
            },
            trust_anchor: {
              configurationFields: [
                "federation_trust_anchor.immediate_subordinates",
                "federation_trust_anchor.trust_anchor_jwks",
              ],
            },
          },
        },
      },
    };
    const { schema, uiSchema } = buildConfigFormSchema(plan, {
      sections: [
        {
          key: "federation_op",
          title: "Federation OP",
          fields: [
            { key: "federation.op_ec_jwks", label: "op_ec_jwks", type: "object" },
            {
              key: "federation.entity_identifier",
              label: "entity_identifier",
              type: "string",
            },
          ],
        },
        {
          key: "federation_trust_anchor",
          title: "Test suite trust anchor",
          fields: [
            {
              key: "federation_trust_anchor.immediate_subordinates",
              label: "immediate_subordinates",
              type: "array",
            },
            {
              key: "federation_trust_anchor.trust_anchor_jwks",
              label: "trust_anchor_jwks",
              type: "string",
            },
          ],
        },
      ],
    });
    // With no variant selected, federation_trust_anchor fields are outside
    // the applicable set, so the section's catalog entries land in the
    // hidden set computed below; the schema/uiSchema also omit it because
    // none of its catalog fields are applicable yet.
    expect(uiSchema.sections.find((s) => s.key === "federation_trust_anchor")).toBeUndefined();
    expect(schema.properties["federation.entity_identifier"]).toBeTruthy();

    const hiddenOp = computeHiddenFields(
      plan,
      { federation_role: "op" },
      {
        sections: [
          {
            key: "federation_trust_anchor",
            title: "ta",
            fields: [
              {
                key: "federation_trust_anchor.immediate_subordinates",
                label: "x",
                type: "array",
              },
              { key: "federation_trust_anchor.trust_anchor_jwks", label: "x", type: "string" },
            ],
          },
        ],
      },
    );
    expect(hiddenOp.has("federation_trust_anchor.immediate_subordinates")).toBe(true);
    expect(hiddenOp.has("federation_trust_anchor.trust_anchor_jwks")).toBe(true);
  });
});
