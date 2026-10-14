package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluations-test-plan",
	shortName = "Batch evaluations",
	certifiable = true,
	displayName = "AuthZEN 1.0: PDP server test for batch evaluations",
	summary = "Every module that expects the PDP to reject its POST request sends an X-Request-ID header, and fails if the error response does not echo it (Section 10.1.3), even when the status is right.",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Batch Evaluations API tests, following the certification scenario (https://openid.github.io/authzen/authorization-api-1_0-certification-scenario.html)
		// Batch Core
		AuthzenPDPEvaluationsBatchRequestWithEvaluationsArrayTest.class,
		AuthzenPDPEvaluationsBatchWithFixtureDecisionsValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithFullySpecifiedEvaluationsTest.class,
		AuthzenPDPEvaluationsBatchWithContextInheritanceTest.class,
		AuthzenPDPEvaluationsEvaluationLevelErrorsTest.class,
		// evaluations_semantic option (Section 7.1.2.1). The execute_all default is what the
		// fixture-decisions module above already relies on; this one asks for it explicitly.
		// The short-circuiting semantics are OPTIONAL for a PDP to support, so requiring them
		// (or requiring an unknown value to be rejected, which presupposes the option is
		// implemented) would fail conformant PDPs that omit the feature; they are covered by
		// AuthzenPDPEvaluationsComprehensiveTestPlan instead.
		AuthzenPDPEvaluationsExecuteAllExplicitTest.class,
		// Per-eval overrides top-level defaults (Section 7.1.1)
		AuthzenPDPEvaluationsPerEvalOverridesDefaultTest.class,
		// No subject at any level: 400, or a decision-false evaluation (Section 7.1, 7.1.2.1)
		AuthzenPDPEvaluationsMissingSubjectTest.class,
		// Backward compatibility (Section 7.1)
		AuthzenPDPEvaluationsMissingEvaluationsArrayBackwardCompatTest.class,
		AuthzenPDPEvaluationsEmptyEvaluationsArrayBackwardCompatTest.class,
		// Forward-compat: unknown top-level fields ignored (Section 10.1.1)
		AuthzenPDPEvaluationsUnknownTopLevelFieldsTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPEvaluationsXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPEvaluationsIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
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
		AuthzenPDPEvaluationsBatchWithTopLevelDefaultInheritanceTest.class,
		// Authentication (Section 11.3)
		AuthzenPDPEvaluationsMissingCredentialsReturns401Test.class,
		AuthzenPDPEvaluationsInvalidCredentialsReturns401Test.class,
	}
)
public class AuthzenPDPEvaluationsTestPlan implements TestPlan {
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		String certProfile = "AuthZEN Evaluations";
		String suffix = "";

		Map<String, String> v = variant.getVariant();
		String authzenSupport = v.get("authzen_support");

		if("properties".equalsIgnoreCase(authzenSupport)) {
			suffix = " w/properties";
		}

		return List.of(certProfile + suffix);
	}
}
