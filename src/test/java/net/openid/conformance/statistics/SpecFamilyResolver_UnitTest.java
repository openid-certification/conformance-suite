package net.openid.conformance.statistics;

import net.openid.conformance.info.Plan;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.variant.VariantService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SpecFamilyResolver_UnitTest {

	/** The 13 {@link SpecFamilyNames} constants, in declaration order. */
	private static final List<String> DECLARED_FAMILIES = List.of(
		SpecFamilyNames.authzen,
		SpecFamilyNames.ekyc,
		SpecFamilyNames.fapi1Advanced,
		SpecFamilyNames.fapi2SecurityProfile,
		SpecFamilyNames.fapi2MessageSigning,
		SpecFamilyNames.fapiCiba,
		SpecFamilyNames.federation,
		SpecFamilyNames.oid4vci,
		SpecFamilyNames.oid4vp,
		SpecFamilyNames.oidcc,
		SpecFamilyNames.oidccLogout,
		SpecFamilyNames.oidccSessionManagement,
		SpecFamilyNames.ssf);

	@Test
	void resolvesPlansPresentInTheMap() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of(
			"fapi1-advanced-final-test-plan", SpecFamilyNames.fapi1Advanced,
			"oidcc-basic-certification-test-plan", SpecFamilyNames.oidcc));

		assertThat(resolver.familyForPlan("fapi1-advanced-final-test-plan")).isEqualTo(SpecFamilyNames.fapi1Advanced);
		assertThat(resolver.familyForPlan("oidcc-basic-certification-test-plan")).isEqualTo(SpecFamilyNames.oidcc);
		assertThat(resolver.isKnownPlan("fapi1-advanced-final-test-plan")).isTrue();
	}

	@Test
	void unknownAndNullPlanNamesFallBackToOtherRetired() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("known-plan", SpecFamilyNames.oidcc));

		assertThat(resolver.familyForPlan("retired-plan")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(resolver.familyForPlan(null)).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(resolver.isKnownPlan("retired-plan")).isFalse();
		assertThat(resolver.isKnownPlan(null)).isFalse();
	}

	@Test
	void plansInIsTheInverseOfFamilyForPlan() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of(
			"fapi1-advanced-final-test-plan", SpecFamilyNames.fapi1Advanced,
			"fapi1-advanced-final-client-test-plan", SpecFamilyNames.fapi1Advanced,
			"oidcc-basic-certification-test-plan", SpecFamilyNames.oidcc));

		assertThat(resolver.plansIn(SpecFamilyNames.fapi1Advanced))
			.containsExactlyInAnyOrder("fapi1-advanced-final-test-plan", "fapi1-advanced-final-client-test-plan");
		assertThat(resolver.plansIn(SpecFamilyNames.oidcc)).containsExactly("oidcc-basic-certification-test-plan");
	}

	@Test
	void plansEverInListsTheRegistrysPlansAndTheFamilysAliasedNames() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of(
			"fapi1-advanced-final-test-plan", SpecFamilyNames.fapi1Advanced,
			"oidcc-basic-certification-test-plan", SpecFamilyNames.oidcc), Map.of(), Map.of(),
			Map.of("fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced));

		// what the family can still be run under...
		assertThat(resolver.plansIn(SpecFamilyNames.fapi1Advanced))
			.containsExactly("fapi1-advanced-final-test-plan");
		// ...and what it has ever had runs under, which is what a drill-down must list
		assertThat(resolver.plansEverIn(SpecFamilyNames.fapi1Advanced))
			.containsExactly("fapi1-advanced-final-test-plan", "fapi-rw-id2-test-plan");
		assertThat(resolver.plansEverIn(SpecFamilyNames.oidcc))
			.containsExactly("oidcc-basic-certification-test-plan");
	}

	@Test
	void plansEverInListsAFamilyWhoseOnlyPlanNamesAreRetired() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced));

		assertThat(resolver.plansIn(SpecFamilyNames.fapi1Advanced)).isEmpty();
		assertThat(resolver.plansEverIn(SpecFamilyNames.fapi1Advanced)).containsExactly("fapi-rw-id2-test-plan");
	}

	@Test
	void plansEverInIsEmptyForAFamilyNeitherTheRegistryNorTheAliasMapKnows() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced,
				"was-standalone", SpecFamilyResolver.NO_PLAN));

		assertThat(resolver.plansEverIn(SpecFamilyNames.fapiCiba)).isEmpty();
		assertThat(resolver.plansEverIn("No Such Family")).isEmpty();
		assertThat(resolver.plansEverIn(null)).isEmpty();
		// an alias of a synthetic family is dropped as unusable, so it never lists either
		assertThat(resolver.plansEverIn(SpecFamilyResolver.NO_PLAN)).isEmpty();
		assertThat(resolver.plansEverIn(SpecFamilyResolver.OTHER_RETIRED)).isEmpty();
	}

	@Test
	void plansInIsEmptyForAFamilyWithoutRegisteredPlans() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("known-plan", SpecFamilyNames.oidcc));

		assertThat(resolver.plansIn(SpecFamilyNames.fapiCiba)).isEmpty();
		assertThat(resolver.plansIn(SpecFamilyResolver.NO_PLAN)).isEmpty();
		assertThat(resolver.plansIn(SpecFamilyResolver.OTHER_RETIRED)).isEmpty();
		assertThat(resolver.plansIn("No Such Family")).isEmpty();
		assertThat(resolver.plansIn(null)).isEmpty();
	}

	@Test
	void everyRegisteredPlanIsListedByPlansInOfItsOwnFamily() {
		VariantService variantService = new VariantService(holder -> true);
		SpecFamilyResolver resolver = new SpecFamilyResolver(variantService);

		for (VariantService.TestPlanHolder holder : variantService.getTestPlans()) {
			String planName = holder.info.testPlanName();
			assertThat(resolver.plansIn(resolver.familyForPlan(planName)))
				.as("plansIn(family of %s)", planName).contains(planName);
		}
	}

	@Test
	void familyOrderIsTheDeclaredFamiliesThenTheSyntheticOnes() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("known-plan", SpecFamilyNames.oidcc));

		List<String> order = resolver.familyOrder();

		assertThat(order).startsWith(DECLARED_FAMILIES.toArray(new String[0]));
		assertThat(order).endsWith(SpecFamilyResolver.NO_PLAN, SpecFamilyResolver.OTHER_RETIRED);
		assertThat(order).hasSize(DECLARED_FAMILIES.size() + 2);
		assertThat(order).doesNotHaveDuplicates();
	}

	@Test
	void familyOrderAppendsFamiliesTheRegistryHasButTestPlanDoesNotDeclare() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of(
			"future-plan", "Brand New Spec",
			"oidcc-plan", SpecFamilyNames.oidcc));

		List<String> order = resolver.familyOrder();

		assertThat(order).hasSize(DECLARED_FAMILIES.size() + 3);
		assertThat(order).containsSubsequence(SpecFamilyNames.ssf, "Brand New Spec",
			SpecFamilyResolver.NO_PLAN, SpecFamilyResolver.OTHER_RETIRED);
	}

	@Test
	void everyRegisteredPlanResolvesToAFamilyInFamilyOrder() {
		VariantService variantService = new VariantService(holder -> true);
		SpecFamilyResolver resolver = new SpecFamilyResolver(variantService);
		List<String> order = resolver.familyOrder();

		Collection<VariantService.TestPlanHolder> plans = variantService.getTestPlans();
		assertThat(plans).isNotEmpty();
		for (VariantService.TestPlanHolder holder : plans) {
			String planName = holder.info.testPlanName();
			assertThat(resolver.isKnownPlan(planName)).as("%s is known", planName).isTrue();
			assertThat(order).as("family of %s", planName).contains(resolver.familyForPlan(planName));
		}

		assertThat(resolver.familyForPlan("a-plan-that-was-deleted")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(order).endsWith(SpecFamilyResolver.NO_PLAN, SpecFamilyResolver.OTHER_RETIRED);
	}

	@Test
	void aModuleBelongsToEveryPlanAndEveryFamilyItIsRunUnder() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(
			Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced, "fapi1-client-plan", SpecFamilyNames.fapi1Advanced,
				"oidcc-plan", SpecFamilyNames.oidcc),
			Map.of(),
			Map.of("fapi1-plan", List.of("shared-module", "fapi1-module"),
				"fapi1-client-plan", List.of("fapi1-module"),
				"oidcc-plan", List.of("shared-module")));

		assertThat(resolver.plansForModule("shared-module")).containsExactlyInAnyOrder("fapi1-plan", "oidcc-plan");
		assertThat(resolver.familiesForModule("shared-module"))
			.containsExactlyInAnyOrder(SpecFamilyNames.fapi1Advanced, SpecFamilyNames.oidcc);
		assertThat(resolver.plansForModule("fapi1-module"))
			.containsExactlyInAnyOrder("fapi1-plan", "fapi1-client-plan");
		// two plans of one family: the family is still named once
		assertThat(resolver.familiesForModule("fapi1-module")).containsExactly(SpecFamilyNames.fapi1Advanced);
		assertThat(resolver.plansForModule("retired-module")).isEmpty();
		assertThat(resolver.familiesForModule("retired-module")).isEmpty();
		assertThat(resolver.plansForModule(null)).isEmpty();
		assertThat(resolver.familiesForModule(null)).isEmpty();
	}

	@Test
	void aRetiredPlanTheAliasMapNamesResolvesToItsFamily() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced));

		assertThat(resolver.familyForPlan("fapi-rw-id2-test-plan")).isEqualTo(SpecFamilyNames.fapi1Advanced);
		assertThat(resolver.isAliased("fapi-rw-id2-test-plan")).isTrue();
		// an alias is a name of a plan that is gone, not a plan the suite can run
		assertThat(resolver.isKnownPlan("fapi-rw-id2-test-plan")).isFalse();
		assertThat(resolver.plansIn(SpecFamilyNames.fapi1Advanced)).isEmpty();
	}

	@Test
	void theRegistryAnswersBeforeTheAliasMap() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("oidcc-plan", SpecFamilyNames.fapi1Advanced));

		assertThat(resolver.familyForPlan("oidcc-plan")).isEqualTo(SpecFamilyNames.oidcc);
		assertThat(resolver.isKnownPlan("oidcc-plan")).isTrue();
		// the stale entry is dropped rather than kept as an answer nothing ever reads
		assertThat(resolver.isAliased("oidcc-plan")).isFalse();
	}

	@Test
	void aPlanNeitherTheRegistryNorTheAliasMapNamesIsStillOtherRetired() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced));

		assertThat(resolver.familyForPlan("a-plan-nobody-remembers")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(resolver.isAliased("a-plan-nobody-remembers")).isFalse();
		assertThat(resolver.isAliased(null)).isFalse();
	}

	@Test
	void anAliasOfAFamilyNoChartDrawsIsIgnoredRatherThanThrown() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("brazil-plan", "Open Banking Brasil",
				"fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced));

		assertThat(resolver.familyForPlan("brazil-plan")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(resolver.isAliased("brazil-plan")).isFalse();
		assertThat(resolver.familyOrder()).doesNotContain("Open Banking Brasil");
		// the usable entry beside it is still read
		assertThat(resolver.familyForPlan("fapi-rw-id2-test-plan")).isEqualTo(SpecFamilyNames.fapi1Advanced);
	}

	@Test
	void anAliasOfASyntheticFamilyIsIgnoredRatherThanCharted() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of("oidcc-plan", SpecFamilyNames.oidcc), Map.of(),
			Map.of(), Map.of("was-standalone", SpecFamilyResolver.NO_PLAN,
				"was-retired", SpecFamilyResolver.OTHER_RETIRED));

		// "No plan" is in familyOrder(), so it would otherwise be accepted and would both count
		// a retired plan's runs as standalone and hide the name from the unresolved plan report
		assertThat(resolver.familyForPlan("was-standalone")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(resolver.familyForPlan("was-retired")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(resolver.isAliased("was-standalone")).isFalse();
		assertThat(resolver.isAliased("was-retired")).isFalse();
	}

	@Test
	void theGeneratedAliasFileNamesRetiredPlansOfFamiliesTheChartsHave() throws IOException {
		VariantService variantService = new VariantService(holder -> true);
		SpecFamilyResolver resolver = new SpecFamilyResolver(variantService);
		Properties aliases = new Properties();
		try (InputStream stream = SpecFamilyResolver.class.getResourceAsStream(SpecFamilyResolver.ALIASES)) {
			assertThat(stream).as("%s is on the classpath", SpecFamilyResolver.ALIASES).isNotNull();
			aliases.load(stream);
		}

		assertThat(aliases).isNotEmpty();
		for (String planName : aliases.stringPropertyNames()) {
			String family = aliases.getProperty(planName);
			assertThat(resolver.familyOrder()).as("family of %s", planName).contains(family);
			assertThat(family).as("family of %s", planName)
				.isNotEqualTo(SpecFamilyResolver.NO_PLAN)
				.isNotEqualTo(SpecFamilyResolver.OTHER_RETIRED);
			// a name the suite still publishes belongs in the registry, not in the alias file
			assertThat(resolver.isKnownPlan(planName)).as("%s is still published", planName).isFalse();
			assertThat(resolver.isAliased(planName)).as("%s is aliased", planName).isTrue();
			assertThat(resolver.familyForPlan(planName)).as("family of %s", planName).isEqualTo(family);
			// the drill-down lists it under that family, though the family select does not offer it
			assertThat(resolver.plansEverIn(family)).as("plansEverIn(%s)", family).contains(planName);
			assertThat(resolver.plansIn(family)).as("plansIn(%s)", family).doesNotContain(planName);
		}
	}

	@Test
	void everyModuleOfARegisteredPlanResolvesBackToThatPlanAndItsFamily() {
		VariantService variantService = new VariantService(holder -> true);
		SpecFamilyResolver resolver = new SpecFamilyResolver(variantService);

		int modules = 0;
		for (VariantService.TestPlanHolder holder : variantService.getTestPlans()) {
			String planName = holder.info.testPlanName();
			for (Plan.Module module : holder.getTestModules()) {
				String testName = module.getTestModule();
				assertThat(resolver.plansForModule(testName)).as("plans of %s", testName).contains(planName);
				if (resolver.isKnownPlan(planName)) {
					assertThat(resolver.familiesForModule(testName)).as("families of %s", testName)
						.contains(resolver.familyForPlan(planName));
				}
				modules++;
			}
		}
		assertThat(modules).isNotZero();
		assertThat(resolver.plansForModule("a-module-that-was-deleted")).isEmpty();
	}
}
