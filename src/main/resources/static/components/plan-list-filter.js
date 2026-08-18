/**
 * The plans-listing filter: the `search` / `owner` / `immutable` / `family` /
 * `plan` / `variant.<k>` / `cert` / `from` / `to` parameters `GET /api/plan`
 * accepts, as they travel through `plans.html`.
 *
 * Pure functions only — no DOM, no fetch, no history. `plans.html` reads them
 * out of `location.search` and hands the result to `<cts-plan-list>`, which
 * forwards them to the API and renders one removable chip per active filter.
 * The statistics page's drill-down (`statistics-model.js`'s `drillDownUrl`)
 * is what produces such a URL in the first place, so the parameter names here
 * are the same ones on both sides of the link, and the same ones the server's
 * `PlanListFilter` / `QueryParams` parse.
 *
 * `to` is EXCLUSIVE (`started < to`), which is what the chips have to
 * translate: a period from `2026-05-04` to `2026-05-11` is shown as
 * "4 May 2026 – 10 May 2026", the days a plan in it could have started on.
 * @module plan-list-filter
 */

/** Milliseconds in a day; the only date arithmetic here is a whole day. */
const DAY_MS = 86400000;

/** Matches `QueryParams.VARIANT_PREFIX` on the server. */
const VARIANT_PREFIX = "variant.";

const SEARCH_PARAM = "search";
const OWNER_PARAM = "owner";
const IMMUTABLE_PARAM = "immutable";
const FAMILY_PARAM = "family";
const PLAN_PARAM = "plan";
const CERT_PARAM = "cert";
const FROM_PARAM = "from";
const TO_PARAM = "to";

/**
 * Every parameter this module owns, so a caller can strip them from a URL
 * without knowing their names.
 * @type {Array<string>}
 */
export const FILTER_PARAMS = [
  SEARCH_PARAM,
  OWNER_PARAM,
  IMMUTABLE_PARAM,
  FAMILY_PARAM,
  PLAN_PARAM,
  CERT_PARAM,
  FROM_PARAM,
  TO_PARAM,
];

/** A `YYYY-MM-DD` date, the form both bounds take when the drill-down builds them. */
const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** Month names for the period chip. Fixed rather than `Intl`, so a chip reads the same everywhere. */
const SHORT_MONTHS = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

/**
 * What the plans listing is narrowed to.
 * @typedef {object} PlanListFilter
 * @property {string} search - A term the SERVER matches, or `""`. Not the same
 *   thing as the search box on the page, which narrows only the rows already
 *   fetched: this one is a MongoDB `$text` phrase over the plan name,
 *   description and certification profile, so it matches whole words only.
 * @property {string} owner - The `sub` of the account whose plans to list, or
 *   `""`. Admin-only in effect: anyone else sees only their own plans anyway.
 * @property {string} immutable - `"true"` for only the plans a certification
 *   package was downloaded for, `"false"` for only the rest, or `""` for both.
 * @property {string} family - Spec family display name, or `""`.
 * @property {string} plan - Exact plan name, or `""`.
 * @property {Record<string, string>} variant - Plan-level variant parameter → value.
 * @property {string} cert - One certification profile name, or `""`.
 * @property {string} from - Lower bound on `started`, inclusive, or `""`.
 * @property {string} to - Upper bound on `started`, EXCLUSIVE, or `""`.
 */

/**
 * One removable filter chip.
 * @typedef {object} PlanListChip
 * @property {string} key - Chip identity, and the argument {@link without}
 *   takes: `search`, `owner`, `immutable`, `family`, `plan`, `cert`, `from` (the whole
 *   period, both bounds) or `variant-<name>`. Also the chip's `data-testid`
 *   suffix.
 * @property {string} label - What the chip reads.
 * @property {string} removeLabel - The accessible name of the remove action.
 */

/**
 * @param {string|null|undefined} value - A raw parameter value.
 * @returns {string} It, trimmed, or `""`.
 */
function text(value) {
  return typeof value === "string" ? value.trim() : "";
}

/**
 * `immutable` is the one filter with a fixed set of values. Anything that is
 * neither is dropped rather than passed on, because the server rejects it with
 * a 400 — a hand-edited link should open the listing unfiltered, as every other
 * unusable value here does.
 * @param {string|null|undefined} value - A raw parameter value.
 * @returns {string} `"true"`, `"false"` or `""`.
 */
function flag(value) {
  const flagged = text(value).toLowerCase();
  return flagged === "true" || flagged === "false" ? flagged : "";
}

/** @returns {PlanListFilter} A filter that narrows nothing. */
export function emptyFilter() {
  return {
    search: "",
    owner: "",
    immutable: "",
    family: "",
    plan: "",
    variant: {},
    cert: "",
    from: "",
    to: "",
  };
}

/**
 * Read the filter out of a query string. Unknown parameters (`public`, and
 * anything a future page adds) and blank values are ignored rather than
 * rejected: a hand-edited link must still open the listing.
 * @param {string} [search] - `location.search`, with or without the leading `?`.
 * @returns {PlanListFilter} The filter it describes.
 */
export function planListFilterFromUrl(search) {
  const params = new URLSearchParams(search || "");
  /** @type {Record<string, string>} */
  const variant = {};
  for (const [key, value] of params.entries()) {
    if (!key.startsWith(VARIANT_PREFIX)) continue;
    const name = text(key.slice(VARIANT_PREFIX.length));
    if (name && text(value)) variant[name] = text(value);
  }
  return {
    search: text(params.get(SEARCH_PARAM)),
    owner: text(params.get(OWNER_PARAM)),
    immutable: flag(params.get(IMMUTABLE_PARAM)),
    family: text(params.get(FAMILY_PARAM)),
    plan: text(params.get(PLAN_PARAM)),
    variant,
    cert: text(params.get(CERT_PARAM)),
    from: text(params.get(FROM_PARAM)),
    to: text(params.get(TO_PARAM)),
  };
}

/**
 * The variant parameters of a filter, alphabetically, so the same filter
 * always produces the same request and the same chip order however the URL
 * ordered them.
 * @param {PlanListFilter} filter - The filter.
 * @returns {Array<[string, string]>} Sorted `[name, value]` pairs.
 */
function variantEntries(filter) {
  return Object.entries((filter && filter.variant) || {})
    .filter(([name, value]) => text(name) !== "" && text(value) !== "")
    .sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
}

/**
 * @param {PlanListFilter} filter - The filter.
 * @returns {boolean} True when it narrows anything at all.
 */
export function hasFilters(filter) {
  if (!filter) return false;
  return Boolean(
    text(filter.search) ||
    text(filter.owner) ||
    text(filter.immutable) ||
    text(filter.family) ||
    text(filter.plan) ||
    text(filter.cert) ||
    text(filter.from) ||
    text(filter.to) ||
    variantEntries(filter).length > 0,
  );
}

/**
 * The filter as `GET /api/plan` parameters. Only the filter's own — the
 * caller adds paging, ordering and `public`.
 * @param {PlanListFilter} filter - The filter.
 * @returns {URLSearchParams} The parameters, empty when nothing is filtered.
 */
export function toParams(filter) {
  const params = new URLSearchParams();
  if (text(filter && filter.search)) params.set(SEARCH_PARAM, text(filter.search));
  if (text(filter && filter.owner)) params.set(OWNER_PARAM, text(filter.owner));
  if (text(filter && filter.immutable)) params.set(IMMUTABLE_PARAM, text(filter.immutable));
  if (text(filter && filter.family)) params.set(FAMILY_PARAM, text(filter.family));
  if (text(filter && filter.plan)) params.set(PLAN_PARAM, text(filter.plan));
  for (const [name, value] of variantEntries(filter)) {
    params.set(VARIANT_PREFIX + text(name), text(value));
  }
  if (text(filter && filter.cert)) params.set(CERT_PARAM, text(filter.cert));
  if (text(filter && filter.from)) params.set(FROM_PARAM, text(filter.from));
  if (text(filter && filter.to)) params.set(TO_PARAM, text(filter.to));
  return params;
}

/**
 * The query string the page should be at for this filter, preserving every
 * parameter the filter does not own (`public`, above all — dropping it would
 * switch the listing back to My).
 * @param {PlanListFilter} filter - The filter.
 * @param {string} [search] - The current `location.search`.
 * @returns {string} A query string starting with `?`, or `""` when nothing at
 *   all is left in it.
 */
export function urlFromFilter(filter, search = "") {
  const params = new URLSearchParams(search || "");
  for (const key of [...params.keys()]) {
    if (FILTER_PARAMS.includes(key) || key.startsWith(VARIANT_PREFIX)) params.delete(key);
  }
  for (const [key, value] of toParams(filter).entries()) params.append(key, value);
  const query = params.toString();
  return query ? `?${query}` : "";
}

/**
 * The filter with one chip's worth of narrowing removed. `from` removes the
 * whole period — the two bounds are one chip, because half a period is not a
 * filter anyone asked for.
 * @param {PlanListFilter} filter - The filter.
 * @param {string} key - A {@link PlanListChip} `key`.
 * @returns {PlanListFilter} A new filter; the input is not mutated.
 */
export function without(filter, key) {
  const next = { ...emptyFilter(), ...(filter || {}) };
  next.variant = { ...(next.variant || {}) };
  if (key === "search") next.search = "";
  else if (key === "owner") next.owner = "";
  else if (key === "immutable") next.immutable = "";
  else if (key === "family") next.family = "";
  else if (key === "plan") next.plan = "";
  else if (key === "cert") next.cert = "";
  else if (key === "from") {
    next.from = "";
    next.to = "";
  } else if (key.startsWith("variant-")) delete next.variant[key.slice("variant-".length)];
  return next;
}

/**
 * A `YYYY-MM-DD` date as "4 May 2026". Anything else (an ISO instant, junk)
 * is shown as it stands — the chip's job is to be recognisable, not to
 * reformat a bound the page did not build.
 * @param {string} value - The bound.
 * @returns {string} A human date, or the input.
 */
function formatDay(value) {
  if (!DATE_RE.test(value)) return value;
  const month = SHORT_MONTHS[Number(value.slice(5, 7)) - 1];
  if (!month) return value;
  return `${Number(value.slice(8, 10))} ${month} ${value.slice(0, 4)}`;
}

/**
 * The day before an exclusive `to` bound — the last day a plan in the range
 * could have started on, which is the only end date worth showing a reader.
 * @param {string} value - The exclusive bound.
 * @returns {string} The inclusive end, or `""` when the bound is not a date.
 */
function inclusiveEnd(value) {
  if (!DATE_RE.test(value)) return "";
  const day = new Date(`${value}T00:00:00Z`);
  if (Number.isNaN(day.getTime())) return "";
  return new Date(day.getTime() - DAY_MS).toISOString().slice(0, 10);
}

/**
 * How the period chip reads, for the four combinations of bounds.
 * @param {string} from - Inclusive lower bound, or `""`.
 * @param {string} to - Exclusive upper bound, or `""`.
 * @returns {string} The chip value, without the leading "Started".
 */
function periodValue(from, to) {
  const end = inclusiveEnd(to);
  if (from && end) {
    return from === end ? formatDay(from) : `${formatDay(from)} – ${formatDay(end)}`;
  }
  if (from && to) return `${formatDay(from)} – before ${formatDay(to)}`;
  if (from) return `on or after ${formatDay(from)}`;
  if (end) return `on or before ${formatDay(end)}`;
  return `before ${formatDay(to)}`;
}

/**
 * One chip per active filter, in a fixed order (search, owner, family, plan,
 * variants, cert, immutable, period) so the row does not reshuffle as filters
 * are removed.
 * @param {PlanListFilter} filter - The filter.
 * @returns {Array<PlanListChip>} The chips.
 */
export function toChips(filter) {
  /** @type {Array<PlanListChip>} */
  const chips = [];
  /**
   * @param {string} key - The chip key.
   * @param {string} name - What the filter is called.
   * @param {string} value - What it is set to.
   * @param {string} [joiner] - What separates the two in the label.
   * @returns {void}
   */
  const add = (key, name, value, joiner = ": ") => {
    const label = `${name}${joiner}${value}`;
    chips.push({
      key,
      label,
      // The accessible name CONTAINS the visible label verbatim (WCAG 2.5.3
      // label in name), so "click Family: OID4VP" works for someone driving
      // the page by voice and the announcement matches what is on screen.
      removeLabel: `Remove filter: ${label}`,
    });
  };

  if (text(filter && filter.search)) add("search", "Search", text(filter.search));
  if (text(filter && filter.owner)) add("owner", "Owner", text(filter.owner));
  if (text(filter && filter.family)) add("family", "Family", text(filter.family));
  if (text(filter && filter.plan)) add("plan", "Plan", text(filter.plan));
  for (const [name, value] of variantEntries(filter)) add(`variant-${name}`, name, value);
  if (text(filter && filter.cert)) add("cert", "Certification profile", text(filter.cert));
  const immutable = text(filter && filter.immutable);
  if (immutable) add("immutable", "Immutable", immutable === "true" ? "yes" : "no");
  const from = text(filter && filter.from);
  const to = text(filter && filter.to);
  if (from || to) add("from", "Started", periodValue(from, to), " ");
  return chips;
}
