package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluation-test-plan",
	displayName = "Authzen 1.0: PDP server test for evaluation - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Evaluation API tests from https://github.com/openid/authzen/issues/433
		// Basic Core
		AuthzenPDPEvaluationFixtureRequestPermitDecisionTest.class,
		AuthzenPDPEvaluationFixtureRequestDenyDecisionTest.class,
		AuthzenPDPEvaluationRequestWithOptionalContextTest.class,
		AuthzenPDPEvaluationRequestWithAdditionalPropertiesTest.class,
		AuthzenPDPEvaluationRequestWithUnknownFieldsTest.class,
		// Basic Properties (Properties variant only)
		AuthzenPDPEvaluationFixtureRequestDenyBasedOnPropertiesTest.class,
		AuthzenPDPEvaluationFixtureRequestPermitBasedOnSubjectPropertiesTest.class,
		AuthzenPDPEvaluationFixtureRequestPermitBasedOnActionPropertiesTest.class,
		AuthzenPDPEvaluationFixtureRequestDenyBasedOnActionPropertiesTest.class,
	}
)
public class AuthzenPDPEvaluationTestPlan implements TestPlan {
}
