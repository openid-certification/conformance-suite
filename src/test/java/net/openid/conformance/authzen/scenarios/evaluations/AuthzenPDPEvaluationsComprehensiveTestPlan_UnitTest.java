package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The comprehensive plan is defined as the certification-track plan plus the OPTIONAL
 * Section 7.1.2.1 short-circuiting semantics. Java annotations cannot compose arrays, so the
 * module list is duplicated by hand; these tests are what stops the two lists drifting apart
 * when a module is added to only one of them.
 */
class AuthzenPDPEvaluationsComprehensiveTestPlan_UnitTest {

	/** Modules that are deliberately absent from the certification-track plan. */
	private static final Set<Class<? extends TestModule>> OPTIONAL_SEMANTIC_MODULES = Set.of(
		AuthzenPDPEvaluationsDenyOnFirstDenyTest.class,
		AuthzenPDPEvaluationsPermitOnFirstPermitTest.class,
		AuthzenPDPEvaluationsUnknownSemanticValueTest.class
	);

	private static List<Class<? extends TestModule>> modulesOf(Class<?> planClass) {
		PublishTestPlan plan = planClass.getAnnotation(PublishTestPlan.class);
		assertTrue(plan != null, planClass.getSimpleName() + " is missing @PublishTestPlan");
		return List.of(plan.testModules());
	}

	@Test
	public void comprehensivePlanContainsEveryCertificationPlanModule() {
		List<Class<? extends TestModule>> comprehensive = modulesOf(AuthzenPDPEvaluationsComprehensiveTestPlan.class);
		List<Class<? extends TestModule>> certification = modulesOf(AuthzenPDPEvaluationsTestPlan.class);

		List<String> missing = certification.stream()
			.filter(m -> !comprehensive.contains(m))
			.map(Class::getSimpleName)
			.collect(Collectors.toList());

		assertTrue(missing.isEmpty(),
			"AuthzenPDPEvaluationsComprehensiveTestPlan must be a superset of AuthzenPDPEvaluationsTestPlan; missing: "
				+ missing);
	}

	@Test
	public void comprehensivePlanAddsOnlyTheOptionalSemanticModules() {
		List<Class<? extends TestModule>> comprehensive = modulesOf(AuthzenPDPEvaluationsComprehensiveTestPlan.class);
		List<Class<? extends TestModule>> certification = modulesOf(AuthzenPDPEvaluationsTestPlan.class);

		Set<Class<? extends TestModule>> extra = comprehensive.stream()
			.filter(m -> !certification.contains(m))
			.collect(Collectors.toSet());

		assertEquals(OPTIONAL_SEMANTIC_MODULES, extra,
			"The only modules the comprehensive plan may add are the OPTIONAL Section 7.1.2.1 semantics");
	}

	@Test
	public void certificationPlanDoesNotRequireTheOptionalSemantics() {
		List<Class<? extends TestModule>> certification = modulesOf(AuthzenPDPEvaluationsTestPlan.class);

		for (Class<? extends TestModule> module : OPTIONAL_SEMANTIC_MODULES) {
			assertFalse(certification.contains(module),
				module.getSimpleName() + " requires OPTIONAL evaluations_semantic support, so it must not be in "
					+ "AuthzenPDPEvaluationsTestPlan");
		}
	}

	@Test
	public void comprehensivePlanHasNoDuplicateModules() {
		List<Class<? extends TestModule>> comprehensive = modulesOf(AuthzenPDPEvaluationsComprehensiveTestPlan.class);
		assertEquals(comprehensive.size(), Set.copyOf(comprehensive).size(),
			"AuthzenPDPEvaluationsComprehensiveTestPlan lists the same module twice");
	}
}
