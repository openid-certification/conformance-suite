/**
 * Ordering and grouping for `<cts-test-selector>`'s plan list.
 *
 * Pure functions only — no DOM. Within a selected spec family the selector
 * shows plans under one heading per entity under test (the plan's `profile`,
 * relabelled through `ENTITY_LABELS`), so each row only needs the plan's
 * `shortName`, which leads with the version where the family has several.
 * Outside a family the list is flat and sorted by family, then full display
 * name. Each row's status badges come from `statusBadges`.
 * @module test-selector-grouping
 */

/**
 * @typedef {object} SelectorPlan
 * @property {string} planName - Unique plan identifier.
 * @property {string} [displayName] - Full name, including spec and version.
 * @property {string} [shortName] - Name within its family and entity.
 * @property {string} [profile] - Entity under test (`TestPlan.ProfileNames`).
 * @property {string} [specFamily] - Spec family the plan belongs to.
 * @property {string} [specVersion] - Spec version, when the family has several.
 * @property {boolean} [certifiable] - False when outside the certification program.
 * @property {boolean} [alpha] - True for alpha plans.
 */

/**
 * @typedef {object} PlanGroup
 * @property {string} label - Entity heading for the group.
 * @property {SelectorPlan[]} plans - The group's plans, in display order.
 */

/**
 * @typedef {object} PlanGrouping
 * @property {boolean} headed - Whether the family spans more than one entity,
 *   so each group is shown under its heading.
 * @property {PlanGroup[]} groups - Non-empty groups, in display order.
 */

/**
 * @typedef {object} StatusBadge
 * @property {"pass" | "warn" | "neutral"} variant - `cts-badge` variant.
 * @property {string} label - Badge text.
 */

/**
 * Group headings for the `TestPlan.ProfileNames` strings. The profile strings
 * themselves can't change (`fintechlabs.profiles.visible` filters on them).
 * Both relying-party profiles share a label so a family never shows two
 * "Relying Party" headings; `CorrectnessTests.pickerEntity` mirrors that merge
 * when it checks that no two rows in a group share a short name.
 * @type {Readonly<Record<string, string>>}
 */
export const ENTITY_LABELS = Object.freeze({
  "Test an OpenID Provider / Authorization Server": "OpenID Provider / Authorization Server",
  "Test a Relying Party / OAuth2 Client": "Relying Party / Client",
  "Test a Relying Party / OAuth2 Client Logout Support": "Relying Party / Client",
  "Test a OpenID4VCI issuer": "OpenID4VCI Issuer",
  "Test a OpenID4VCI wallet": "OpenID4VCI Wallet",
  "Test a OpenID4VP wallet": "OpenID4VP Wallet",
  "Test a OpenID4VP Verifier": "OpenID4VP Verifier",
});

/**
 * @param {string | undefined} profile - A plan's `profile`.
 * @returns {string} The heading for that entity; an unmapped profile is shown as is.
 */
export function entityLabel(profile) {
  return (profile && ENTITY_LABELS[profile]) || profile || "";
}

/**
 * Newest version first: a "Final" version ahead of the drafts, then
 * descending, with plans that carry no version last.
 * @param {string | undefined} a - A `specVersion`.
 * @param {string | undefined} b - Another `specVersion`.
 * @returns {number} Comparator result.
 */
export function compareVersions(a, b) {
  const va = a || "";
  const vb = b || "";
  if (va === vb) return 0;
  if (!va) return 1;
  if (!vb) return -1;
  const finalA = va.includes("Final");
  const finalB = vb.includes("Final");
  if (finalA !== finalB) return finalA ? -1 : 1;
  return vb.localeCompare(va, undefined, { numeric: true });
}

/**
 * Order within one entity: newest version first, then certifiable before not,
 * non-alpha before alpha, then by name. Within a version the plan most testers
 * want comes first.
 * @param {SelectorPlan} a - A plan.
 * @param {SelectorPlan} b - Another plan.
 * @returns {number} Comparator result.
 */
export function comparePlansInGroup(a, b) {
  const byVersion = compareVersions(a.specVersion, b.specVersion);
  if (byVersion !== 0) return byVersion;
  const certA = a.certifiable === false ? 1 : 0;
  const certB = b.certifiable === false ? 1 : 0;
  if (certA !== certB) return certA - certB;
  const alphaA = a.alpha ? 1 : 0;
  const alphaB = b.alpha ? 1 : 0;
  if (alphaA !== alphaB) return alphaA - alphaB;
  return rowName(a).localeCompare(rowName(b));
}

/**
 * The status badges a plan row carries: "Certification" for a certifiable
 * plan, then "Alpha" for an alpha one, so a certifiable alpha plan carries
 * both. A plan whose payload has no certification status carries none.
 * @param {SelectorPlan} plan - A plan.
 * @returns {StatusBadge[]} The badges to show, in order.
 */
export function statusBadges(plan) {
  /** @type {StatusBadge[]} */
  const badges = [];
  if (plan.certifiable) badges.push({ variant: "pass", label: "Certification" });
  if (plan.alpha) badges.push({ variant: "warn", label: "Alpha" });
  return badges;
}

/**
 * Whether the selector lists a plan while alpha plans are hidden: every plan
 * except those outside the certification program. A plan whose payload has
 * no certification status is listed.
 * @param {SelectorPlan} plan - A plan.
 * @returns {boolean} True when the plan is listed by default.
 */
export function isListedByDefault(plan) {
  return plan.certifiable !== false;
}

/**
 * @param {SelectorPlan} plan - A plan.
 * @returns {string} The name shown on the plan's row inside its family.
 */
export function rowName(plan) {
  return plan.shortName || plan.displayName || plan.planName;
}

/**
 * Group a family's visible plans by entity.
 *
 * Whether headings show is decided on the whole family, not on what the
 * search left visible, so headings don't come and go while the user types.
 * @param {SelectorPlan[]} familyPlans - Every plan in the selected family.
 * @param {SelectorPlan[]} visiblePlans - The subset to show (search applied).
 * @returns {PlanGrouping} The groups to render.
 */
export function groupPlans(familyPlans, visiblePlans) {
  const familyLabels = new Set(familyPlans.map((p) => entityLabel(p.profile)));
  const headed = familyLabels.size > 1;
  /** @type {Map<string, SelectorPlan[]>} */
  const byLabel = new Map();
  for (const plan of visiblePlans) {
    const label = headed ? entityLabel(plan.profile) : "";
    const members = byLabel.get(label);
    if (members) members.push(plan);
    else byLabel.set(label, [plan]);
  }
  const groups = Array.from(byLabel, ([label, plans]) => ({
    label,
    plans: plans.slice().sort(comparePlansInGroup),
  })).sort((a, b) => a.label.localeCompare(b.label));
  return { headed, groups };
}

/**
 * Deterministic order for the flat "All specifications" list: by family, then
 * full name. `/api/plan/available` returns the plans in no particular order.
 * @param {SelectorPlan[]} plans - Plans to order.
 * @returns {SelectorPlan[]} A sorted copy.
 */
export function sortFlat(plans) {
  return plans
    .slice()
    .sort(
      (a, b) =>
        (a.specFamily || "").localeCompare(b.specFamily || "") ||
        (a.displayName || a.planName).localeCompare(b.displayName || b.planName),
    );
}
