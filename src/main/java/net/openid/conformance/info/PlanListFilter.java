package net.openid.conformance.info;

import net.openid.conformance.statistics.QueryParams;
import net.openid.conformance.statistics.SpecFamilyResolver;
import net.openid.conformance.statistics.VariantKeys;
import net.openid.conformance.variant.VariantSelection;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The optional narrowing a caller of {@code GET /api/plan} asked for: which plans, which
 * plan level variant, which certification profile and which slice of time the listing should
 * be restricted to. It is what the statistics page drills down with when a chart bar is
 * clicked.
 *
 * <p>This only ever <b>narrows</b> a listing. The scoping of that listing - all plans for an
 * admin, your own plans otherwise, published plans only for {@code ?public=true} - is decided
 * by {@link DBTestPlanService} and is not expressible here, so no combination of these
 * parameters can widen what a caller is allowed to see.
 *
 * <p>Comparing {@code started} is a string comparison, because that is how the field is
 * stored: an ISO-8601 UTC instant, which sorts chronologically as text. A {@code YYYY-MM-DD}
 * bound therefore covers the whole of that day, {@code from} is inclusive and {@code to} is
 * exclusive, so the bounds of adjacent periods can be passed straight through from a chart.
 *
 * <p>That only works because every bound is turned into a <b>prefix</b> of the timestamps it
 * bounds: a date stays as it is, and a timestamp is truncated to its second and stripped of
 * its 'Z' - {@code 2026-06-02T15:54:10.162Z} becomes {@code 2026-06-02T15:54:10}. Otherwise
 * the comparison would not be one of instants at all within a single second, because
 * {@code Instant.toString()} leaves the fraction out when there is none and '.' sorts before
 * 'Z', which would put {@code …:10.162Z} below a bound of {@code …:10Z} even though it is
 * later. The cost of the truncation is that a timestamp bound is honoured to the second:
 * the second it names is wholly included by {@code from} and wholly excluded by {@code to}.
 *
 * @param planNames the plan names to list, or null for all of them; empty matches nothing,
 *                  which is what an unrecognised spec family resolves to
 * @param variant   plan level variant parameters that all have to be set to these values
 * @param cert      one certification profile the plan has to be certified against, or null
 * @param immutable whether to list only the plans a certification package has been downloaded
 *                  for ({@code true}) or only those it has not ({@code false}), or null for
 *                  both
 * @param from      the earliest {@code started} to include, inclusive, or null
 * @param to        the {@code started} to stop before, exclusive, or null
 */
public record PlanListFilter(Set<String> planNames, Map<String, String> variant, String cert, Boolean immutable,
							 String from, String to) {

	private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

	/** The plan level variant selection itself. */
	private static final String VARIANT = "variant";

	/** Where a plan whose variant is a plain legacy string may keep that string. */
	private static final String LEGACY_VARIANT_PATH = VARIANT + "." + VariantSelection.LEGACY_VARIANT_NAME;

	public PlanListFilter {
		planNames = planNames == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(planNames));
		variant = variant == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(variant));
	}

	/**
	 * @param params   the request parameters, as {@code HttpServletRequest.getParameterMap()}
	 *                 returns them; parameters this does not understand (the paging ones, a
	 *                 cache buster) are ignored
	 * @param families what {@code family} is resolved against: every plan name that family has
	 *                 ever had, the retired ones included - see {@link #planNames}
	 * @return the filter they describe; blank values count as absent, so a client can send an
	 *         empty parameter for "no filter"
	 * @throws IllegalArgumentException if a value cannot be used; the message names the
	 *                                  parameter and is safe to show to the caller
	 */
	public static PlanListFilter parse(Map<String, String[]> params, SpecFamilyResolver families) {
		return new PlanListFilter(
			planNames(QueryParams.first(params, "family"), QueryParams.first(params, "plan"), families),
			QueryParams.variant(params),
			QueryParams.first(params, "cert"),
			flag("immutable", QueryParams.first(params, "immutable")),
			bound("from", QueryParams.first(params, "from")),
			bound("to", QueryParams.first(params, "to")));
	}

	/** @return true if this narrows nothing, so the listing can be left exactly as it was */
	public boolean isEmpty() {
		return planNames == null && variant.isEmpty() && cert == null && immutable == null
			&& from == null && to == null;
	}

	/** @return a clause per active filter, to be added to the criteria that scope the listing */
	public Criteria toCriteria() {
		Criteria criteria = new Criteria();
		if (planNames != null) {
			if (planNames.size() == 1) {
				criteria.and("planName").is(planNames.iterator().next());
			} else {
				// a list, not the set itself, so the query document is a plain BSON array
				criteria.and("planName").in(List.copyOf(planNames));
			}
		}
		// keyed by field path rather than by parameter name, because two parameter names -
		// `legacy` and the stored field it stands for - mean the same field, and Criteria
		// cannot hold the same path twice
		Map<String, String> byPath = new LinkedHashMap<>();
		variant.forEach((name, value) -> byPath.putIfAbsent(variantPath(name), value));
		byPath.forEach((path, value) -> {
			if (LEGACY_VARIANT_PATH.equals(path)) {
				criteria.orOperator(legacyVariantIs(value));
			} else {
				criteria.and(path).is(value);
			}
		});
		if (cert != null) {
			// certificationProfileName is an array; equality matches any one of its elements
			criteria.and("certificationProfileName").is(cert);
		}
		if (immutable != null) {
			// a plan is immutable only when the field is there and true: it is written when a
			// certification package is downloaded and is absent on every plan before that, so
			// "not immutable" is $ne rather than $eq false, which matches a missing field too
			Criteria clause = criteria.and("immutable");
			if (immutable) {
				clause.is(true);
			} else {
				clause.ne(true);
			}
		}
		if (from != null || to != null) {
			Criteria started = criteria.and("started");
			if (from != null) {
				started.gte(from);
			}
			if (to != null) {
				started.lt(to);
			}
		}
		return criteria;
	}

	/**
	 * @param name a variant parameter name as it was sent
	 * @return the field path of that plan level variant parameter, which happens to read
	 *         like the request parameter itself - {@code variant.fapi_profile} - for every
	 *         name but one: the statistics cube calls the plain string a legacy plan stores
	 *         instead of a variant selection {@value VariantKeys#LEGACY}
	 *         ({@link VariantKeys#canonical}), so a drill-down into one of those cells
	 *         arrives as {@code variant.legacy=<string>} and has to be matched against where
	 *         the plan really keeps it - see {@link #legacyVariantIs}. Matching
	 *         {@code variant.legacy} would list nothing, because no plan document has such a
	 *         field.
	 */
	private static String variantPath(String name) {
		return VariantKeys.LEGACY.equals(name) ? LEGACY_VARIANT_PATH : VARIANT + "." + name;
	}

	/**
	 * A legacy variant is stored in either of two shapes, because that is what the suite
	 * reads back (see {@code VariantConverters}): the plain string it always was
	 * ({@code variant: "<string>"}), and the one-entry sub-document a
	 * {@link VariantSelection} of one is written out as
	 * ({@code variant: {__variant__: "<string>"}}). Both canonicalise to the same
	 * {@code legacy=<string>} cube cell ({@link VariantKeys#canonical} handles a
	 * {@code CharSequence} and a map alike), so a drill-down into that cell has to list both
	 * or it lists only half of what was counted.
	 *
	 * <p>Not exhaustive for every conceivable value: the cube key replaces {@code ;} and
	 * {@code =} in a legacy string with {@code _}, so a value containing either cannot make
	 * the round trip back into a filter. That is pre-existing and cannot be fixed here - the
	 * client only ever sees the sanitised form.
	 *
	 * @param value the legacy variant string to match
	 * @return the two clauses to OR together
	 */
	private static Criteria[] legacyVariantIs(String value) {
		return new Criteria[] { Criteria.where(VARIANT).is(value), Criteria.where(LEGACY_VARIANT_PATH).is(value) };
	}

	/**
	 * A family is resolved with {@link SpecFamilyResolver#plansEverIn(String)} rather than
	 * {@link SpecFamilyResolver#plansIn(String)}, because this is the other half of a
	 * drill-down: the chart bar that was clicked counted the runs of the family's retired and
	 * renamed plan names as well as of the plans the suite still publishes, so a listing that
	 * named only the latter would show fewer plans than the bar it came from - and the older
	 * the period, the more of them missing. The family select on the statistics page still
	 * offers only what the registry has; what is listed here is what was counted.
	 *
	 * @return the plan names both parameters allow, or null if neither was sent; an empty set
	 *         when they cannot both be satisfied, so an unknown family - or a plan that is not
	 *         in the family asked for - lists nothing rather than everything
	 */
	private static Set<String> planNames(String family, String plan, SpecFamilyResolver families) {
		if (family == null) {
			return plan == null ? null : Set.of(plan);
		}
		Set<String> inFamily = families.plansEverIn(family);
		if (plan == null) {
			return inFamily;
		}
		return inFamily.contains(plan) ? Set.of(plan) : Set.of();
	}

	/**
	 * @param parameter the request parameter the value came from, for the error message
	 * @param value     a date or a timestamp with a time zone
	 * @return a prefix of every stored {@code started} the bound covers: a date as it stands,
	 *         a timestamp in UTC - so an offset does not break the comparison - truncated to
	 *         its second and without the trailing 'Z'
	 * @throws IllegalArgumentException if it is neither
	 */
	/**
	 * @param parameter the request parameter the value came from, for the error message
	 * @param value     {@code true} or {@code false}, in any case, or null
	 * @return that as a {@link Boolean}, or null if it was not sent
	 * @throws IllegalArgumentException if it is neither, rather than silently reading anything
	 *                                  that is not "true" as false
	 */
	private static Boolean flag(String parameter, String value) {
		if (value == null) {
			return null;
		}
		if ("true".equalsIgnoreCase(value)) {
			return Boolean.TRUE;
		}
		if ("false".equalsIgnoreCase(value)) {
			return Boolean.FALSE;
		}
		throw new IllegalArgumentException("%s must be true or false, not '%s'".formatted(parameter, value));
	}

	private static String bound(String parameter, String value) {
		if (value == null) {
			return null;
		}
		if (DATE.matcher(value).matches()) {
			try {
				return LocalDate.parse(value).toString();
			} catch (DateTimeParseException e) {
				// the right shape but not a real date, e.g. 2026-02-30; reported below
			}
		} else {
			try {
				String instant = OffsetDateTime.parse(value).toInstant().truncatedTo(ChronoUnit.SECONDS).toString();
				return instant.substring(0, instant.length() - 1); // the trailing 'Z'
			} catch (DateTimeParseException e) {
				// not a timestamp either; reported below
			}
		}
		throw new IllegalArgumentException(("%s must be a date (YYYY-MM-DD) or a timestamp with a time zone "
			+ "(e.g. 2026-06-01T00:00:00Z), not '%s'").formatted(parameter, value));
	}

}
