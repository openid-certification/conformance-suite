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
		// Error handling: HTTP 400 negative tests (Section 2.4)
		AuthzenPDPEvaluationMissingSubjectTest.class,
		AuthzenPDPEvaluationMissingActionTest.class,
		AuthzenPDPEvaluationMissingResourceTest.class,
		AuthzenPDPEvaluationSubjectMissingTypeTest.class,
		AuthzenPDPEvaluationSubjectMissingIdTest.class,
		AuthzenPDPEvaluationActionMissingNameTest.class,
		AuthzenPDPEvaluationResourceMissingTypeTest.class,
		AuthzenPDPEvaluationResourceMissingIdTest.class,
		AuthzenPDPEvaluationSubjectIsStringTest.class,
		AuthzenPDPEvaluationActionNameIsNumberTest.class,
		// X-Request-ID handling (Section 2.5)
		AuthzenPDPEvaluationXRequestIdEchoedTest.class,
		// Idempotency (Section 2.6)
		AuthzenPDPEvaluationIdempotencyTest.class,
		// Forward-compat: unknown fields ignored (Spec 10.1.1-3)
		AuthzenPDPEvaluationUnknownTopLevelFieldsTest.class,
		AuthzenPDPEvaluationUnknownSubjectFieldsTest.class,
		AuthzenPDPEvaluationUnknownResourceFieldsTest.class,
		AuthzenPDPEvaluationUnknownActionFieldsTest.class,
		// Transport binding negative tests (Spec 10.1.1 / 10.1.2 / 2.4.4 / 2.4.5)
		AuthzenPDPEvaluationRejectGetMethodTest.class,
		AuthzenPDPEvaluationRejectPutMethodTest.class,
		AuthzenPDPEvaluationRejectTopLevelArrayTest.class,
		AuthzenPDPEvaluationRejectNonJsonContentTypeTest.class,
		AuthzenPDPEvaluationAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPEvaluationRejectMalformedJsonTest.class,
		AuthzenPDPEvaluationRejectEmptyBodyTest.class,
		// Authentication (Section 11.3)
		AuthzenPDPEvaluationMissingCredentialsReturns401Test.class,
	}
)
public class AuthzenPDPEvaluationTestPlan implements TestPlan {
}
