import { describe, it, expect } from "vitest";
import {
  FILTER_PARAMS,
  emptyFilter,
  hasFilters,
  planListFilterFromUrl,
  toChips,
  toParams,
  urlFromFilter,
  without,
} from "./plan-list-filter.js";

/** Nothing at all, where a filter is expected — the defensive path. */
const NOTHING = /** @type {any} */ (null);

/**
 * The one chip a period-only filter produces.
 * @param {string} from - Inclusive lower bound, or `""`.
 * @param {string} to - Exclusive upper bound, or `""`.
 * @returns {import("./plan-list-filter.js").PlanListChip} The period chip.
 */
function periodChip(from, to) {
  return toChips({ ...emptyFilter(), from, to })[0];
}

/** The filter a drill-down from a monthly bar produces. */
function drillDownFilter() {
  return {
    // a drill-down narrows nobody's plans in particular, but the fixture is a
    // whole filter: `without` normalises what it is given, so a partial one
    // would differ from its own result by the keys it left out
    search: "",
    owner: "",
    immutable: "",
    family: "FAPI-CIBA",
    plan: "fapi-ciba-id1-test-plan",
    variant: { fapi_profile: "openbanking_brazil", client_auth_type: "mtls" },
    cert: "FAPI-CIBA: Poll w/ MTLS",
    from: "2026-05-01",
    to: "2026-06-01",
  };
}

describe("planListFilterFromUrl", () => {
  it("reads every parameter the drill-down writes", () => {
    const filter = planListFilterFromUrl(
      "?family=FAPI-CIBA&plan=fapi-ciba-id1-test-plan" +
        "&variant.fapi_profile=openbanking_brazil&variant.client_auth_type=mtls" +
        "&cert=FAPI-CIBA%3A+Poll+w%2F+MTLS&from=2026-05-01&to=2026-06-01",
    );
    expect(filter).toEqual(drillDownFilter());
  });

  it("ignores parameters it does not own, blanks and a bare variant prefix", () => {
    const filter = planListFilterFromUrl(
      "?public=true&family=+OIDCC+&plan=&cert=%20&variant.=x&variant.profile=&range=12m",
    );
    expect(filter).toEqual({ ...emptyFilter(), family: "OIDCC" });
    expect(hasFilters(filter)).toBe(true);
  });

  it("reads an empty or absent query as no filter at all", () => {
    expect(planListFilterFromUrl("")).toEqual(emptyFilter());
    expect(planListFilterFromUrl(undefined)).toEqual(emptyFilter());
    expect(hasFilters(planListFilterFromUrl("?public=true"))).toBe(false);
    expect(hasFilters(NOTHING)).toBe(false);
  });
});

describe("toParams", () => {
  it("names every filter, with the variants in a stable order", () => {
    expect(toParams(drillDownFilter()).toString()).toBe(
      "family=FAPI-CIBA&plan=fapi-ciba-id1-test-plan" +
        "&variant.client_auth_type=mtls&variant.fapi_profile=openbanking_brazil" +
        "&cert=FAPI-CIBA%3A+Poll+w%2F+MTLS&from=2026-05-01&to=2026-06-01",
    );
  });

  it("is empty when nothing is filtered", () => {
    expect(toParams(emptyFilter()).toString()).toBe("");
    expect(toParams(NOTHING).toString()).toBe("");
  });

  it("round-trips through a URL", () => {
    const filter = drillDownFilter();
    expect(planListFilterFromUrl(`?${toParams(filter)}`)).toEqual(filter);
  });
});

describe("urlFromFilter", () => {
  it("keeps the parameters the filter does not own", () => {
    // Dropping `public` would switch the listing back to My behind the user's
    // back, which is the one collision that actually happens on this page.
    expect(
      urlFromFilter({ ...emptyFilter(), family: "OIDCC" }, "?public=true&family=FAPI-CIBA"),
    ).toBe("?public=true&family=OIDCC");
  });

  it("really removes a cleared filter rather than leaving it empty", () => {
    const url = urlFromFilter(
      without(drillDownFilter(), "from"),
      `?${toParams(drillDownFilter())}`,
    );
    expect(url).not.toContain("from=");
    expect(url).not.toContain("to=");
    expect(url).toContain("family=FAPI-CIBA");
  });

  it("is the empty string when nothing is left", () => {
    expect(urlFromFilter(emptyFilter(), "?family=OIDCC&variant.x=y")).toBe("");
  });

  it("names every parameter this module owns", () => {
    expect(FILTER_PARAMS).toEqual([
      "search",
      "owner",
      "immutable",
      "family",
      "plan",
      "cert",
      "from",
      "to",
    ]);
  });
});

describe("without", () => {
  it("removes one filter and leaves the rest alone", () => {
    const filter = drillDownFilter();
    expect(without(filter, "family").family).toBe("");
    expect(without(filter, "plan").plan).toBe("");
    expect(without(filter, "cert").cert).toBe("");
    expect(without(filter, "variant-fapi_profile").variant).toEqual({ client_auth_type: "mtls" });
    // The input is untouched: the component keeps rendering the old filter
    // until the new one is assigned.
    expect(filter).toEqual(drillDownFilter());
  });

  it("removes both bounds with the period chip", () => {
    const next = without(drillDownFilter(), "from");
    expect(next.from).toBe("");
    expect(next.to).toBe("");
  });

  it("ignores a key that names nothing", () => {
    expect(without(drillDownFilter(), "nonsense")).toEqual(drillDownFilter());
  });
});

describe("toChips", () => {
  it("orders the chips owner, family, plan, variants, cert, immutable, period", () => {
    expect(
      toChips({ ...drillDownFilter(), owner: "developer", immutable: "false" }).map(
        (chip) => chip.key,
      ),
    ).toEqual([
      "owner",
      "family",
      "plan",
      "variant-client_auth_type",
      "variant-fapi_profile",
      "cert",
      "immutable",
      "from",
    ]);
  });

  it("orders the chips family, plan, variants, cert, period", () => {
    expect(toChips(drillDownFilter()).map((chip) => chip.key)).toEqual([
      "family",
      "plan",
      "variant-client_auth_type",
      "variant-fapi_profile",
      "cert",
      "from",
    ]);
  });

  it("labels each chip and names its remove action", () => {
    const chips = toChips(drillDownFilter());
    expect(chips[0]).toEqual({
      key: "family",
      label: "Family: FAPI-CIBA",
      removeLabel: "Remove filter: Family: FAPI-CIBA",
    });
    expect(chips[2].label).toBe("client_auth_type: mtls");
    expect(chips[4].label).toBe("Certification profile: FAPI-CIBA: Poll w/ MTLS");
    // WCAG 2.5.3 label in name: the accessible name of every chip contains
    // its visible label verbatim, so a voice command that says what is on
    // screen activates it.
    for (const chip of chips) {
      expect(chip.removeLabel).toBe(`Remove filter: ${chip.label}`);
    }
  });

  it("shows the period as the days a plan in it could have started on", () => {
    // `to` is exclusive, so the last day shown is the day before it — a
    // reader must never be told a range ends on a day it excludes.
    const chip = periodChip("2026-05-04", "2026-05-11");
    expect(chip.label).toBe("Started 4 May 2026 – 10 May 2026");
    expect(chip.removeLabel).toBe("Remove filter: Started 4 May 2026 – 10 May 2026");
  });

  it("shows a single-day period as one date", () => {
    expect(periodChip("2026-05-04", "2026-05-05").label).toBe("Started 4 May 2026");
  });

  it("shows a month the way the drill-down sends it", () => {
    expect(periodChip("2026-05-01", "2026-06-01").label).toBe("Started 1 May 2026 – 31 May 2026");
  });

  it("handles a half-open period and a bound that is not a date", () => {
    expect(periodChip("2026-05-04", "").label).toBe("Started on or after 4 May 2026");
    expect(periodChip("", "2026-05-11").label).toBe("Started on or before 10 May 2026");
    // An ISO instant is a legal bound on the API; it is shown as it stands
    // rather than being silently reinterpreted a day earlier.
    expect(periodChip("2026-05-04T09:00:00Z", "").label).toBe(
      "Started on or after 2026-05-04T09:00:00Z",
    );
  });

  it("has nothing to show for an empty filter", () => {
    expect(toChips(emptyFilter())).toEqual([]);
    expect(toChips(NOTHING)).toEqual([]);
  });
});

describe("owner and immutable", () => {
  it("reads both from a URL and sends them back as parameters", () => {
    const filter = planListFilterFromUrl("?owner=104383237143811096540&immutable=true");

    expect(filter.owner).toBe("104383237143811096540");
    expect(filter.immutable).toBe("true");
    expect(toParams(filter).toString()).toBe("owner=104383237143811096540&immutable=true");
  });

  it("accepts immutable in any case, as the server does", () => {
    expect(planListFilterFromUrl("?immutable=TRUE").immutable).toBe("true");
    expect(planListFilterFromUrl("?immutable=False").immutable).toBe("false");
  });

  it("drops an immutable that is neither true nor false", () => {
    // the server rejects it with a 400, so sending it on would break the page
    // for a hand-edited link rather than just ignoring the unusable bit
    expect(planListFilterFromUrl("?immutable=yes").immutable).toBe("");
    expect(hasFilters(planListFilterFromUrl("?immutable=yes"))).toBe(false);
  });

  it("counts each of them as narrowing the listing", () => {
    expect(hasFilters({ ...emptyFilter(), owner: "developer" })).toBe(true);
    expect(hasFilters({ ...emptyFilter(), immutable: "false" })).toBe(true);
  });

  it("removes either one on its own", () => {
    const filter = { ...emptyFilter(), owner: "developer", immutable: "true" };

    expect(without(filter, "owner")).toEqual({ ...filter, owner: "" });
    expect(without(filter, "immutable")).toEqual({ ...filter, immutable: "" });
  });

  it("says yes or no rather than true or false on the chip", () => {
    expect(toChips({ ...emptyFilter(), immutable: "true" })[0].label).toBe("Immutable: yes");
    expect(toChips({ ...emptyFilter(), immutable: "false" })[0].label).toBe("Immutable: no");
    expect(toChips({ ...emptyFilter(), owner: "developer" })[0].label).toBe("Owner: developer");
  });
});

describe("server-side search", () => {
  it("round-trips as a filter of its own", () => {
    const filter = planListFilterFromUrl("?search=ciba");

    expect(filter.search).toBe("ciba");
    expect(hasFilters(filter)).toBe(true);
    expect(toParams(filter).toString()).toBe("search=ciba");
  });

  it("is the first chip, and reads as a search", () => {
    const chips = toChips({ ...emptyFilter(), search: "ciba", owner: "developer" });

    expect(chips.map((chip) => chip.key)).toEqual(["search", "owner"]);
    expect(chips[0].label).toBe("Search: ciba");
  });

  it("can be removed on its own", () => {
    const filter = { ...emptyFilter(), search: "ciba", owner: "developer" };

    expect(without(filter, "search")).toEqual({ ...filter, search: "" });
  });
});
