package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluations-test-plan",
	displayName = "Authzen 1.0: PDP server test for batch evaluations - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Batch Evaluations API tests from https://github.com/openid/authzen/issues/433
		// Batch Core
		AuthzenPDPEvaluationsBatchRequestWithEvaluationsArrayTest.class,
		AuthzenPDPEvaluationsBatchWithFixtureDecisionsValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithFullySpecifiedEvaluationsTest.class,
		AuthzenPDPEvaluationsBatchWithContextInheritanceTest.class,
		AuthzenPDPEvaluationsEvaluationLevelErrorsTest.class,
		// X-Request-ID handling (Spec 10.1.3-4)
		AuthzenPDPEvaluationsXRequestIdEchoedTest.class,
		// Batch Properties (Properties variant only)
		AuthzenPDPEvaluationsBatchWithPropertiesValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithSubjectPropertiesValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithDefaultValueMergingTest.class,
	}
)
public class AuthzenPDEvaluationsTestPlan implements TestPlan {
}
