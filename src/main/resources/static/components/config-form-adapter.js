/**
 * Pure functions that turn the conformance suite's per-plan API payload
 * plus the static `config-field-catalog.json` into the schema/uiSchema/
 * hiddenFields trio that `<cts-config-form>` consumes.
 *
 * The adapter exists because `/api/plan/info/{planName}` (and the cached
 * `/api/plan/available` superset that the schedule-test page already
 * holds) only returns dotted-path strings under `configurationFields`
 * and `hidesConfigurationFields`. The human metadata — section grouping,
 * per-field labels, type hints, tooltips, placeholders, the JWKS-generator
 * markers — lives only in the catalog. Both inputs are required.
 *
 * Adapter functions are pure and synchronous:
 * - Same inputs always produce the same outputs.
 * - No fetch, no DOM access, no module-level mutation.
 * - Safe to call from a render loop or from a variant-change listener.
 *
 * The intended consumer is `schedule-test.html`'s `updateConfigFieldVisibility`,
 * which already reads `FAPI_UI.availablePlans[planName]` to compute today's
 * DOM walk. The adapter computes the same applicability set and routes it
 * through `<cts-config-form>` instead of toggling element styles.
 */

/**
 * @typedef {object} CatalogField
 * @property {string} key - Full dotted data-json-target path (e.g. "client.client_id").
 * @property {string} label - Human-readable label.
 * @property {string} type - "string" | "object" | "array" | "boolean".
 * @property {string} [format] - "password" | "uri" | "newline-array" | etc.
 * @property {string[]} [enum] - Allowed values for select fields.
 * @property {string} [placeholder] - Placeholder text.
 * @property {boolean} [required] - HTML required flag.
 * @property {string} [jwksGenerator] - Slug for the inline JWKS generator button.
 * @property {string} [tooltip] - Help text shown under the field.
 */

/**
 * @typedef {object} CatalogSection
 * @property {string} key - UI identifier for the section.
 * @property {string} title - Section heading.
 * @property {string} [intro] - Optional descriptive prose under the heading.
 * @property {CatalogField[]} fields
 */

/**
 * @typedef {object} FieldCatalog
 * @property {CatalogSection[]} sections
 */

/**
 * @typedef {object} VariantValueInfo
 * @property {string[]} [configurationFields]
 * @property {string[]} [hidesConfigurationFields]
 */

/**
 * @typedef {object} VariantInfo
 * @property {Record<string, VariantValueInfo>} [variantValues]
 */

/**
 * @typedef {object} ModuleInfo
 * @property {string[]} [configurationFields]
 */

/**
 * @typedef {object} PlanInfo
 * @property {string} [planName]
 * @property {string[]} [configurationFields]
 * @property {string[]} [hidesConfigurationFields]
 * @property {ModuleInfo[]} [modules]
 * @property {Record<string, VariantInfo>} [variants]
 */

/**
 * Fields that are always visible regardless of plan or variant. Mirrors
 * the legacy `updateConfigFieldVisibility` push at the end of the applicable
 * set: `fieldsToShow.push('alias', 'description', 'publish')`.
 */
export const ALWAYS_ON_FIELDS = Object.freeze(["alias", "description", "publish"]);

/**
 * Compute the union of dotted-path strings that should be applicable for this
 * plan, taking the plan-level fields, module-level fields, and (optionally) the
 * currently-selected variant's per-value fields. Always-on fields are added at
 * the end so callers don't need to special-case them.
 *
 * @param {PlanInfo | null | undefined} planInfo
 * @param {Record<string, string>} [selectedVariant] - Map of variant param → selected value.
 * @returns {Set<string>}
 */
export function computeApplicableFields(planInfo, selectedVariant) {
  const out = new Set();
  if (!planInfo) {
    for (const k of ALWAYS_ON_FIELDS) out.add(k);
    return out;
  }
  for (const f of planInfo.configurationFields || []) out.add(f);
  for (const m of planInfo.modules || []) {
    for (const f of m.configurationFields || []) out.add(f);
  }
  if (selectedVariant && planInfo.variants) {
    for (const [param, value] of Object.entries(selectedVariant)) {
      if (!value || value === "select") continue;
      const variantValue = planInfo.variants[param]?.variantValues?.[value];
      for (const f of variantValue?.configurationFields || []) out.add(f);
    }
  }
  for (const f of ALWAYS_ON_FIELDS) out.add(f);
  return out;
}

/**
 * Compute the union of dotted-path strings that should be explicitly hidden for
 * this plan + selected variant. Always-on fields are never hidden, even if a
 * variant declares them in `hidesConfigurationFields`.
 *
 * @param {PlanInfo | null | undefined} planInfo
 * @param {Record<string, string>} [selectedVariant]
 * @returns {Set<string>}
 */
export function computeExplicitHides(planInfo, selectedVariant) {
  const out = new Set();
  if (!planInfo) return out;
  for (const f of planInfo.hidesConfigurationFields || []) out.add(f);
  if (selectedVariant && planInfo.variants) {
    for (const [param, value] of Object.entries(selectedVariant)) {
      if (!value || value === "select") continue;
      const variantValue = planInfo.variants[param]?.variantValues?.[value];
      for (const f of variantValue?.hidesConfigurationFields || []) out.add(f);
    }
  }
  for (const f of ALWAYS_ON_FIELDS) out.delete(f);
  return out;
}

/**
 * Translate a catalog field into the JSON-schema fragment that `cts-form-field`
 * consumes. Carries `x-cts-*` annotations for adapter-specific metadata (JWKS
 * generator slug, newline-array hint, placeholder) that `cts-form-field` itself
 * ignores but page code can read.
 *
 * @param {CatalogField} field
 * @returns {object}
 */
function fieldToSchemaFragment(field) {
  /** @type {Record<string, any>} */
  const out = {
    type: field.type || "string",
    title: field.label || field.key,
  };
  if (field.format) out.format = field.format;
  if (field.enum) out.enum = field.enum.slice();
  if (field.tooltip) out.description = field.tooltip;
  if (field.placeholder) out["x-cts-placeholder"] = field.placeholder;
  if (field.required) out["x-cts-required"] = true;
  if (field.jwksGenerator) out["x-cts-jwks-generator"] = field.jwksGenerator;
  return out;
}

/**
 * Build the `<cts-config-form>` schema/uiSchema pair for a given plan.
 *
 * Output shape uses the component's explicit-fields mode: `schema.properties`
 * is flat-keyed by full dotted path, and `uiSchema.sections[*]` carries the
 * subset of applicable paths that render under that section. Sections with no
 * applicable fields are omitted entirely.
 *
 * Backend `configurationFields` entries that are NOT in the catalog still
 * appear in the schema as `{ type: "string", title: <path> }` fallbacks (so
 * the field renders rather than disappearing) and are appended to a synthetic
 * trailing section titled "Other" so the user can see and edit them. A
 * `console.warn` is logged once per unknown path so a future PR can extend
 * the catalog.
 *
 * @param {PlanInfo} planInfo
 * @param {FieldCatalog} fieldCatalog
 * @returns {{ schema: object, uiSchema: object }}
 */
export function buildConfigFormSchema(planInfo, fieldCatalog) {
  const applicable = computeApplicableFields(planInfo, undefined);
  /** @type {Record<string, any>} */
  const properties = {};
  /** @type {Array<{ key: string, title: string, fields: string[] }>} */
  const sections = [];
  const seen = new Set();

  for (const section of fieldCatalog?.sections || []) {
    /** @type {string[]} */
    const sectionFields = [];
    for (const field of section.fields || []) {
      if (!applicable.has(field.key)) continue;
      properties[field.key] = fieldToSchemaFragment(field);
      sectionFields.push(field.key);
      seen.add(field.key);
    }
    if (sectionFields.length > 0) {
      sections.push({ key: section.key, title: section.title, fields: sectionFields });
    }
  }

  // Surface any backend-known field that the catalog doesn't carry so it
  // still renders with a fallback label rather than vanishing.
  /** @type {string[]} */
  const orphans = [];
  for (const path of applicable) {
    if (seen.has(path)) continue;
    properties[path] = { type: "string", title: path };
    orphans.push(path);
  }
  if (orphans.length > 0) {
    sections.push({ key: "_orphans", title: "Other", fields: orphans });
    console.warn(
      `[config-form-adapter] ${orphans.length} configurationFields not in catalog:`,
      orphans,
    );
  }

  return {
    schema: { type: "object", properties },
    uiSchema: { sections },
  };
}

/**
 * Compute the set of full-path keys to hide in `<cts-config-form>` for the
 * given plan and currently-selected variant. The component filters these from
 * both the Form tab and the JSON tab; consumers do not need to touch
 * `schema.properties`.
 *
 * Hiding rules (mirror today's `updateConfigFieldVisibility`):
 * 1. Every catalog-known path that is NOT in the applicable set is hidden.
 * 2. Every path in `plan.hidesConfigurationFields` or
 *    `variants[…].variantValues[…].hidesConfigurationFields` is hidden
 *    (this fires even when the field IS in the applicable set, because
 *    `hides` always wins).
 * 3. Always-on fields (alias, description, publish) are never hidden.
 *
 * Backend-applicable fields that the catalog doesn't carry are NOT hidden —
 * `buildConfigFormSchema` surfaces them in an "Other" section, and hiding
 * them would defeat the fallback. The caller's `_orphans` section is a
 * fail-safe surface, not a candidate for the visibility computation.
 *
 * @param {PlanInfo} planInfo
 * @param {Record<string, string>} selectedVariant
 * @param {FieldCatalog} fieldCatalog
 * @returns {Set<string>}
 */
export function computeHiddenFields(planInfo, selectedVariant, fieldCatalog) {
  const applicable = computeApplicableFields(planInfo, selectedVariant);
  const explicitHides = computeExplicitHides(planInfo, selectedVariant);
  const hidden = new Set();

  for (const section of fieldCatalog?.sections || []) {
    for (const field of section.fields || []) {
      if (ALWAYS_ON_FIELDS.includes(field.key)) continue;
      if (explicitHides.has(field.key) || !applicable.has(field.key)) {
        hidden.add(field.key);
      }
    }
  }
  return hidden;
}
