package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** What the statistics unit tests build their cubes and queries from. */
final class StatisticsFixtures {

	/** A Thursday; the Monday of its ISO week is 2026-03-09. */
	static final LocalDate NOW = LocalDate.of(2026, 3, 12);

	static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0, 0);

	/** One plan of each of two families: an OP test plan and an RP test plan. */
	static final SpecFamilyResolver RESOLVER = new SpecFamilyResolver(
		Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced, "oidcc-plan", SpecFamilyNames.oidcc),
		Map.of("fapi1-plan", ProfileNames.optest, "oidcc-plan", ProfileNames.rptest));

	private StatisticsFixtures() {
	}

	/**
	 * @param keysAndValues request parameters, alternating name and value
	 * @return the query they parse to; with none, the whole history by month, unfiltered
	 */
	static StatisticsQuery query(String... keysAndValues) {
		Map<String, String[]> params = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			params.put(keysAndValues[i], new String[] {keysAndValues[i + 1]});
		}
		return StatisticsQuery.parse(params);
	}

	/** @return a cell of {@code runs} runs of {@code planName} with no variant and no profile, all passed */
	static RunCell runCell(String month, String week, String planName, long runs) {
		return new RunCell(month, week, planName, false, "", "", runs, 0, 0, 0, 0, 0);
	}
}
