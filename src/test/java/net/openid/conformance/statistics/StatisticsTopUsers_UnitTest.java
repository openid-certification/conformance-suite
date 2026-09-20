package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.statistics.StatisticsOverview.TopUser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.openid.conformance.statistics.StatisticsFixtures.ISSUER;
import static net.openid.conformance.statistics.StatisticsFixtures.NOW;
import static net.openid.conformance.statistics.StatisticsFixtures.NO_TILES;
import static net.openid.conformance.statistics.StatisticsFixtures.moduleRuns;
import static net.openid.conformance.statistics.StatisticsFixtures.query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

/** The top users table of the overview: who ran the most test modules. */
class StatisticsTopUsers_UnitTest {

	private static final String OIDCC = "oidcc-module";

	private static final String FAPI1 = "fapi1-module";

	private static final SpecFamilyResolver RESOLVER = new SpecFamilyResolver(
		Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced, "oidcc-plan", SpecFamilyNames.oidcc),
		Map.of("fapi1-plan", ProfileNames.optest, "oidcc-plan", ProfileNames.rptest),
		Map.of("fapi1-plan", List.of(FAPI1), "oidcc-plan", List.of(OIDCC)));

	@Test
	void aUsersRunsAreSummedOverEveryModuleAndMonthAndTheBusiestComesFirst() {
		List<TopUser> users = topUsers(cube(
			cell("2026-02", OIDCC, 0, 3, 1),
			cell("2026-03", OIDCC, 0, 2, 2),
			cell("2026-03", FAPI1, 0, 1, 0),
			cell("2026-03", OIDCC, 1, 10, 4)), query());

		assertThat(users).extracting(TopUser::iss, TopUser::sub, TopUser::runs, TopUser::failed, TopUser::modules)
			.containsExactly(
				tuple(ISSUER, "user-1", 10L, 4L, 1L),
				tuple(ISSUER, "user-0", 6L, 3L, 2L));
	}

	@Test
	void aTieOnRunsIsBrokenByTheIdentity() {
		StatisticsCube cube = new StatisticsCube(List.of(), List.of(), List.of(), List.of(),
			new ModuleRuns(
				List.of(cell("2026-03", OIDCC, 0, 5, 0), cell("2026-03", OIDCC, 1, 5, 0), cell("2026-03", OIDCC, 2, 5, 0)),
				List.of(new Owner("https://b.example", "1"), new Owner("https://a.example", "2"),
					new Owner("https://a.example", "1"))),
			List.of(), List.of(), NO_TILES, RESOLVER, NOW);

		assertThat(topUsers(cube, query())).extracting(TopUser::iss, TopUser::sub)
			.containsExactly(
				tuple("https://a.example", "1"),
				tuple("https://a.example", "2"),
				tuple("https://b.example", "1"));
	}

	@Test
	void theRangeSelectsWhichMonthsCount() {
		StatisticsCube cube = cube(
			cell("2025-12", OIDCC, 0, 100, 0),
			cell("2026-01", OIDCC, 0, 1, 0),
			cell("2026-02", OIDCC, 1, 2, 0));

		assertThat(topUsers(cube, query("from", "2026-01", "to", "2026-02"))).extracting(TopUser::sub, TopUser::runs)
			.containsExactly(tuple("user-1", 2L), tuple("user-0", 1L));
	}

	@Test
	void theFamilyAndPlanFiltersCountOnlyTheRunsOfTheirModules() {
		StatisticsCube cube = cube(
			cell("2026-03", OIDCC, 0, 9, 0),
			cell("2026-03", FAPI1, 0, 1, 0),
			cell("2026-03", FAPI1, 1, 4, 0));

		assertThat(topUsers(cube, query("family", SpecFamilyNames.fapi1Advanced))).extracting(TopUser::sub, TopUser::runs)
			.containsExactly(tuple("user-1", 4L), tuple("user-0", 1L));
		assertThat(topUsers(cube, query("plan", "oidcc-plan"))).extracting(TopUser::sub, TopUser::runs)
			.containsExactly(tuple("user-0", 9L));
	}

	@Test
	void theVariantAndCertificationFiltersDoNotNarrowTheUsers() {
		StatisticsCube cube = cube(cell("2026-03", OIDCC, 0, 9, 0));

		assertThat(topUsers(cube, query("variant.fapi_profile", "openbanking_brazil", "cert", "No Such Profile")))
			.extracting(TopUser::runs).containsExactly(9L);
	}

	@Test
	void onlyTheTwentyBusiestUsersAreListed() {
		List<ModuleUserCell> cells = new ArrayList<>();
		for (int user = 0; user < UserRanker.TOP + 5; user++) {
			cells.add(cell("2026-03", OIDCC, user, user + 1L, 0));
		}

		List<TopUser> users = topUsers(cube(cells.toArray(new ModuleUserCell[0])), query());

		assertThat(users).hasSize(UserRanker.TOP);
		assertThat(users.get(0).sub()).isEqualTo("user-24");
		assertThat(users.get(UserRanker.TOP - 1).sub()).isEqualTo("user-5");
	}

	@Test
	void anEmptyCubeHasNoTopUsers() {
		assertThat(topUsers(cube(), query())).isEmpty();
	}

	@Test
	void aCellWhoseOwnerIsNotAmongTheOwnersIsRefused() {
		List<ModuleUserCell> cells = List.of(cell("2026-03", OIDCC, 1, 1, 0));
		List<Owner> owners = List.of(new Owner(ISSUER, "only"));

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ModuleRuns(cells, owners))
			.withMessageContaining("owner id 1");
	}

	private static List<TopUser> topUsers(StatisticsCube cube, StatisticsQuery query) {
		return StatisticsSlicer.slice(cube, query).topUsers();
	}

	private static StatisticsCube cube(ModuleUserCell... cells) {
		return new StatisticsCube(List.of(), List.of(), List.of(), List.of(), moduleRuns(cells), List.of(), List.of(),
			NO_TILES, RESOLVER, NOW);
	}

	private static ModuleUserCell cell(String month, String testName, int ownerId, long runs, long failed) {
		return new ModuleUserCell(month, testName, ownerId, runs, failed);
	}
}
