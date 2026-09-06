package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.CertProfile;
import net.openid.conformance.statistics.StatisticsOverview.Dimensions;
import net.openid.conformance.statistics.StatisticsOverview.Entity;
import net.openid.conformance.statistics.StatisticsOverview.PlanDimension;
import net.openid.conformance.statistics.StatisticsOverview.VariantValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Counts what the filter selects can offer - plans, variant values, certification profiles
 * and entities under test - under the query currently being sliced, so that narrowing one
 * filter narrows the choices left in the others.
 *
 * <p>Each dimension is counted with its <em>own</em> filter left out (the usual faceted
 * search rule): the plan select is counted under the query minus its plan, a variant
 * parameter's select under the query minus that parameter, and so on. Otherwise picking a
 * plan would leave the plan select offering that one plan, and the only way to a sibling
 * would be to clear the filter first. Entities are not a filter and are counted under the
 * whole query.
 *
 * <p>Users are counted <b>distinct</b>: a user who ran twenty plans with the same variant
 * value counts once for it. Plans are the secondary count, which is what makes the
 * difference between "one enthusiast" and "everybody" visible in the same chart.
 *
 * <p>A value is offered as soon as any cell mentions it, even if nothing has been counted
 * against it under the current filter, so a select never hides a value the data has.
 */
final class DimensionCounter {

	private DimensionCounter() {
	}

	/**
	 * @param cube        the cube being sliced
	 * @param query       the query being sliced
	 * @param granularity the granularity being sliced at
	 * @param periods     the periods on the axis; cells outside them are not counted
	 * @return the dimensions of the current slice
	 */
	static Dimensions count(StatisticsCube cube, StatisticsQuery query, Granularity granularity, Set<String> periods) {
		Facets facets = new Facets(cube, query);
		Map<String, long[]> plans = new LinkedHashMap<>();
		Map<String, Long> entities = new LinkedHashMap<>();
		Map<String, Map<String, Counts>> variants = new TreeMap<>();
		Map<String, Counts> certProfiles = new LinkedHashMap<>();

		for (RunCell cell : cube.runs(granularity)) {
			if (!periods.contains(cell.period(granularity))) {
				continue;
			}
			boolean all = facets.all.matches(cell);
			if (cell.planName() != null && (all || facets.plans.matches(cell))) {
				plans.computeIfAbsent(cell.planName(), plan -> new long[2])[0] += cell.runs();
			}
			if (all) {
				entities.merge(facets.all.entityOf(cell), cell.runs(), Long::sum);
			}
			// offer the values even when no plan or user has been counted against them yet
			for (Map.Entry<String, String> parameter : cube.variantOf(cell.variantKey()).entrySet()) {
				if (all || facets.variant(parameter.getKey()).matches(cell)) {
					counts(variants, parameter.getKey(), parameter.getValue());
				}
			}
			if (all || facets.certs.matches(cell)) {
				counts(certProfiles, cell.certKey());
			}
		}

		for (PlanCell cell : cube.plans(granularity)) {
			if (!periods.contains(cell.period(granularity))) {
				continue;
			}
			boolean all = facets.all.matches(cell);
			if (cell.planName() != null && (all || facets.plans.matches(cell))) {
				plans.computeIfAbsent(cell.planName(), plan -> new long[2])[1] += cell.plans();
			}
			for (Map.Entry<String, String> parameter : cube.variantOf(cell.variantKey()).entrySet()) {
				if (all || facets.variant(parameter.getKey()).matches(cell)) {
					counts(variants, parameter.getKey(), parameter.getValue()).plans += cell.plans();
				}
			}
			if (all || facets.certs.matches(cell)) {
				Counts profile = counts(certProfiles, cell.certKey());
				if (profile != null) {
					profile.plans += cell.plans();
				}
			}
		}

		for (UserTuple tuple : cube.users()) {
			if (!active(tuple, granularity, periods)) {
				continue;
			}
			boolean all = facets.all.matches(tuple);
			for (Map.Entry<String, String> parameter : cube.variantOf(tuple.variantKey()).entrySet()) {
				if (all || facets.variant(parameter.getKey()).matches(tuple)) {
					counts(variants, parameter.getKey(), parameter.getValue()).users.add(tuple.ownerId());
				}
			}
			if (all || facets.certs.matches(tuple)) {
				Counts profile = counts(certProfiles, tuple.certKey());
				if (profile != null) {
					profile.users.add(tuple.ownerId());
				}
			}
		}

		return new Dimensions(plans(cube, plans), variants(variants), certProfiles(certProfiles), entities(entities));
	}

	/**
	 * The filter each dimension is counted under: the whole query for the entities, and the
	 * query minus its own filter for every select. A cell the whole query matches is matched
	 * by every relaxed filter too, so the callers test the relaxed ones only when it does not.
	 */
	private static final class Facets {

		private final CellFilter all;

		private final CellFilter plans;

		private final CellFilter certs;

		private final Map<String, CellFilter> variants = new HashMap<>();

		private Facets(StatisticsCube cube, StatisticsQuery query) {
			this.all = new CellFilter(cube, query);
			this.plans = new CellFilter(cube, query.withoutPlan());
			this.certs = new CellFilter(cube, query.withoutCert());
			for (String parameter : query.variant().keySet()) {
				variants.put(parameter, new CellFilter(cube, query.withoutVariant(parameter)));
			}
		}

		/** @return the filter a variant parameter's select is counted under */
		private CellFilter variant(String parameter) {
			// a parameter that is not filtered on has nothing to leave out
			return variants.getOrDefault(parameter, all);
		}
	}

	private static boolean active(UserTuple tuple, Granularity granularity, Set<String> periods) {
		for (String period : tuple.periods(granularity)) {
			if (periods.contains(period)) {
				return true;
			}
		}
		return false;
	}

	private static Counts counts(Map<String, Map<String, Counts>> variants, String parameter, String value) {
		return variants.computeIfAbsent(parameter, key -> new TreeMap<>()).computeIfAbsent(value, key -> new Counts());
	}

	/** @return the counts of a certification profile, or null for the plans that have none */
	private static Counts counts(Map<String, Counts> certProfiles, String certKey) {
		if (certKey == null || certKey.isEmpty()) {
			return null;
		}
		return certProfiles.computeIfAbsent(certKey, key -> new Counts());
	}

	private static List<PlanDimension> plans(StatisticsCube cube, Map<String, long[]> plans) {
		List<PlanDimension> dimensions = new ArrayList<>(plans.size());
		plans.forEach((planName, counts) ->
			dimensions.add(new PlanDimension(planName, cube.familyOf(planName), counts[0], counts[1])));
		dimensions.sort(Comparator.comparingLong(PlanDimension::runs).reversed()
			.thenComparing(Comparator.comparingLong(PlanDimension::plans).reversed())
			.thenComparing(PlanDimension::planName));
		return List.copyOf(dimensions);
	}

	private static Map<String, List<VariantValue>> variants(Map<String, Map<String, Counts>> variants) {
		Map<String, List<VariantValue>> dimensions = new LinkedHashMap<>();
		variants.forEach((parameter, values) -> {
			List<VariantValue> counted = new ArrayList<>(values.size());
			values.forEach((value, counts) -> counted.add(new VariantValue(value, counts.users.size(), counts.plans)));
			counted.sort(Comparator.comparingLong(VariantValue::users).reversed()
				.thenComparing(Comparator.comparingLong(VariantValue::plans).reversed())
				.thenComparing(VariantValue::value));
			dimensions.put(parameter, List.copyOf(counted));
		});
		// unmodifiableMap rather than copyOf: the parameter names stay in alphabetical order
		return Collections.unmodifiableMap(dimensions);
	}

	private static List<CertProfile> certProfiles(Map<String, Counts> certProfiles) {
		List<CertProfile> dimensions = new ArrayList<>(certProfiles.size());
		certProfiles.forEach((name, counts) ->
			dimensions.add(new CertProfile(name, counts.users.size(), counts.plans)));
		dimensions.sort(Comparator.comparingLong(CertProfile::users).reversed()
			.thenComparing(Comparator.comparingLong(CertProfile::plans).reversed())
			.thenComparing(CertProfile::name));
		return List.copyOf(dimensions);
	}

	private static List<Entity> entities(Map<String, Long> entities) {
		List<Entity> dimensions = new ArrayList<>(entities.size());
		entities.forEach((entity, runs) -> dimensions.add(new Entity(entity, runs)));
		dimensions.sort(Comparator.comparingLong(Entity::runs).reversed().thenComparing(Entity::entity));
		return List.copyOf(dimensions);
	}

	/** The two counts every value dimension carries: distinct users, and plans. */
	private static final class Counts {

		private final Set<Integer> users = new HashSet<>();

		private long plans;
	}
}
