package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.CertProfile;
import net.openid.conformance.statistics.StatisticsOverview.Dimensions;
import net.openid.conformance.statistics.StatisticsOverview.Entity;
import net.openid.conformance.statistics.StatisticsOverview.PlanDimension;
import net.openid.conformance.statistics.StatisticsOverview.VariantValue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Counts what the filter selects can offer - plans, variant values, certification profiles
 * and entities under test - under the query currently being sliced, so that narrowing one
 * filter narrows the choices left in the others.
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
	 * @param filter      the query's cell filter
	 * @param granularity the granularity being sliced at
	 * @param periods     the periods on the axis; cells outside them are not counted
	 * @return the dimensions of the current slice
	 */
	static Dimensions count(StatisticsCube cube, CellFilter filter, Granularity granularity, Set<String> periods) {
		Map<String, long[]> plans = new LinkedHashMap<>();
		Map<String, Long> entities = new LinkedHashMap<>();
		Map<String, Map<String, Counts>> variants = new TreeMap<>();
		Map<String, Counts> certProfiles = new LinkedHashMap<>();

		for (RunCell cell : cube.runs(granularity)) {
			if (!filter.matches(cell) || !periods.contains(cell.period(granularity))) {
				continue;
			}
			if (cell.planName() != null) {
				plans.computeIfAbsent(cell.planName(), plan -> new long[2])[0] += cell.runs();
			}
			entities.merge(filter.entityOf(cell), cell.runs(), Long::sum);
			// offer the values even when no plan or user has been counted against them yet
			for (Map.Entry<String, String> parameter : cube.variantOf(cell.variantKey()).entrySet()) {
				counts(variants, parameter.getKey(), parameter.getValue());
			}
			counts(certProfiles, cell.certKey());
		}

		for (PlanCell cell : cube.plans(granularity)) {
			if (!filter.matches(cell) || !periods.contains(cell.period(granularity))) {
				continue;
			}
			if (cell.planName() != null) {
				plans.computeIfAbsent(cell.planName(), plan -> new long[2])[1] += cell.plans();
			}
			for (Map.Entry<String, String> parameter : cube.variantOf(cell.variantKey()).entrySet()) {
				counts(variants, parameter.getKey(), parameter.getValue()).plans += cell.plans();
			}
			Counts profile = counts(certProfiles, cell.certKey());
			if (profile != null) {
				profile.plans += cell.plans();
			}
		}

		for (UserTuple tuple : cube.users()) {
			if (!filter.matches(tuple) || !active(tuple, granularity, periods)) {
				continue;
			}
			for (Map.Entry<String, String> parameter : cube.variantOf(tuple.variantKey()).entrySet()) {
				counts(variants, parameter.getKey(), parameter.getValue()).users.set(tuple.ownerId());
			}
			Counts profile = counts(certProfiles, tuple.certKey());
			if (profile != null) {
				profile.users.set(tuple.ownerId());
			}
		}

		return new Dimensions(plans(cube, plans), variants(variants), certProfiles(certProfiles), entities(entities));
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
		return ranked(dimensions, byCounts(PlanDimension::runs, PlanDimension::plans, PlanDimension::planName));
	}

	private static Map<String, List<VariantValue>> variants(Map<String, Map<String, Counts>> variants) {
		Map<String, List<VariantValue>> dimensions = new LinkedHashMap<>();
		variants.forEach((parameter, values) -> {
			List<VariantValue> counted = new ArrayList<>(values.size());
			values.forEach((value, counts) -> counted.add(new VariantValue(value, counts.users.cardinality(), counts.plans)));
			dimensions.put(parameter, ranked(counted, byCounts(VariantValue::users, VariantValue::plans, VariantValue::value)));
		});
		// unmodifiableMap rather than copyOf: the parameter names stay in alphabetical order
		return Collections.unmodifiableMap(dimensions);
	}

	private static List<CertProfile> certProfiles(Map<String, Counts> certProfiles) {
		List<CertProfile> dimensions = new ArrayList<>(certProfiles.size());
		certProfiles.forEach((name, counts) ->
			dimensions.add(new CertProfile(name, counts.users.cardinality(), counts.plans)));
		return ranked(dimensions, byCounts(CertProfile::users, CertProfile::plans, CertProfile::name));
	}

	private static List<Entity> entities(Map<String, Long> entities) {
		List<Entity> dimensions = new ArrayList<>(entities.size());
		entities.forEach((entity, runs) -> dimensions.add(new Entity(entity, runs)));
		return ranked(dimensions, Comparator.comparingLong(Entity::runs).reversed().thenComparing(Entity::entity));
	}

	/** @return {@code rows} in {@code order}, frozen */
	private static <T> List<T> ranked(List<T> rows, Comparator<T> order) {
		rows.sort(order);
		return List.copyOf(rows);
	}

	/** @return biggest first count first, then biggest second count, then the name, so the list is the same every request */
	private static <T> Comparator<T> byCounts(ToLongFunction<T> first, ToLongFunction<T> second, Function<T, String> name) {
		return Comparator.comparingLong(first).reversed()
			.thenComparing(Comparator.comparingLong(second).reversed())
			.thenComparing(name);
	}

	/**
	 * The two counts every value dimension carries: distinct users, and plans. The users are
	 * a bit set: {@link OwnerIds} hands out dense ids from zero, so a user is one bit rather
	 * than a boxed Integer in a hash set.
	 */
	private static final class Counts {

		private final BitSet users = new BitSet();

		private long plans;
	}
}
