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
		// evaluations_semantic option (Spec 7.1.2.1)
		AuthzenPDPEvaluationsDefaultSemanticIsExecuteAllTest.class,
		AuthzenPDPEvaluationsExecuteAllExplicitTest.class,
		AuthzenPDPEvaluationsDenyOnFirstDenyTest.class,
		AuthzenPDPEvaluationsPermitOnFirstPermitTest.class,
		AuthzenPDPEvaluationsUnknownSemanticValueTest.class,
		// Per-eval overrides top-level defaults (Spec 7.1.1.1)
		AuthzenPDPEvaluationsPerEvalOverridesDefaultTest.class,
		// Missing subject everywhere returns decision-false evaluation (Cert Profile 3.4.1)
		AuthzenPDPEvaluationsMissingSubjectReturnsDecisionFalseTest.class,
		// X-Request-ID handling (Spec 10.1.3-4)
		AuthzenPDPEvaluationsXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPEvaluationsIdempotencyTest.class,
		// Transport binding negative tests (Spec 10.1.1 / 10.1.2 / 2.4.4 / 2.4.5)
		AuthzenPDPEvaluationsRejectGetMethodTest.class,
		AuthzenPDPEvaluationsRejectPutMethodTest.class,
		AuthzenPDPEvaluationsRejectTopLevelArrayTest.class,
		AuthzenPDPEvaluationsRejectNonJsonContentTypeTest.class,
		AuthzenPDPEvaluationsAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPEvaluationsRejectMalformedJsonTest.class,
		AuthzenPDPEvaluationsRejectEmptyBodyTest.class,
		// Batch Properties (Properties variant only)
		AuthzenPDPEvaluationsBatchWithPropertiesValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithSubjectPropertiesValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithDefaultValueMergingTest.class,
	}
)
public class AuthzenPDEvaluationsTestPlan implements TestPlan {
}
