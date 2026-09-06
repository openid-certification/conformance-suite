package net.openid.conformance.statistics;

import java.util.Map;

/**
 * Decides which cells of a {@link StatisticsCube} a {@link StatisticsQuery} selects, and
 * resolves the family and entity a run cell is attributed to. Shared by the slicer and the
 * dimension counter so that the charts and the filter selects can never disagree about what
 * the current filter matches.
 */
final class CellFilter {

	private final StatisticsCube cube;

	private final StatisticsQuery query;

	CellFilter(StatisticsCube cube, StatisticsQuery query) {
		this.cube = cube;
		this.query = query;
	}

	boolean matches(RunCell cell) {
		return matches(familyOf(cell), cell.planName(), cell.variantKey(), cell.certKey());
	}

	boolean matches(PlanCell cell) {
		return matches(cube.familyOf(cell.planName()), cell.planName(), cell.variantKey(), cell.certKey());
	}

	boolean matches(UserTuple tuple) {
		return matches(cube.familyOf(tuple.planName()), tuple.planName(), tuple.variantKey(), tuple.certKey());
	}

	/** @return the family the runs are charted under; see {@link StatisticsCube#familyOfRuns} */
	String familyOf(RunCell cell) {
		return cube.familyOfRuns(cell);
	}

	/** @return what the runs were testing: standalone runs are not tied to a plan's profile */
	String entityOf(RunCell cell) {
		return cell.standalone() ? SpecFamilyResolver.NO_PLAN : cube.entityOf(cell.planName());
	}

	private boolean matches(String family, String planName, String variantKey, String certKey) {
		if (query.family() != null && !query.family().equals(family)) {
			return false;
		}
		if (query.plan() != null && !query.plan().equals(planName)) {
			return false;
		}
		if (query.cert() != null && !query.cert().equals(certKey)) {
			return false;
		}
		if (query.variant().isEmpty()) {
			return true;
		}
		Map<String, String> variant = cube.variantOf(variantKey);
		for (Map.Entry<String, String> filter : query.variant().entrySet()) {
			if (!filter.getValue().equals(variant.get(filter.getKey()))) {
				return false;
			}
		}
		return true;
	}
}
