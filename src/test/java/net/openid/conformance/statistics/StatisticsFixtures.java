package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** What the statistics unit tests build their cubes and queries from. */
final class StatisticsFixtures {

	/** A Thursday; the Monday of its ISO week is 2026-03-09. */
	static final LocalDate NOW = LocalDate.of(2026, 3, 12);

	static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0, 0);

	/** The issuer of every user {@link #moduleRuns} makes up. */
	static final String ISSUER = "https://idp.example";

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

	/**
	 * @param cells module cells, with whatever owner ids the test finds convenient
	 * @return the cells with an owner for every id up to the highest one used: owner id
	 *         {@code n} is {@code user-n} at {@link #ISSUER}
	 */
	static ModuleRuns moduleRuns(ModuleUserCell... cells) {
		int highest = Arrays.stream(cells).mapToInt(ModuleUserCell::ownerId).max().orElse(-1);
		List<Owner> owners = IntStream.rangeClosed(0, highest).mapToObj(id -> new Owner(ISSUER, "user-" + id)).toList();
		return new ModuleRuns(List.of(cells), owners);
	}

	/** @return a cell of {@code runs} runs of {@code planName} with no variant and no profile, all passed */
	static RunCell runCell(String month, String week, String planName, long runs) {
		return new RunCell(month, week, planName, false, "", "", runs, 0, 0, 0, 0, 0);
	}
}
