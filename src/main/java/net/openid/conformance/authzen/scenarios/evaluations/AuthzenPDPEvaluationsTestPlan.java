package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluations-test-plan",
	displayName = "AuthZEN 1.0: PDP server test for batch evaluations - alpha tests (not currently part of certification program)",
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
		// evaluations_semantic option (Section 7.1.2.1). The execute_all default is what the
		// fixture-decisions module above already relies on; this one asks for it explicitly.
		// The short-circuiting semantics are OPTIONAL for a PDP to support, so requiring them
		// (or requiring an unknown value to be rejected, which presupposes the option is
		// implemented) would fail conformant PDPs that omit the feature; they are covered by
		// AuthzenPDPEvaluationsComprehensiveTestPlan instead.
		AuthzenPDPEvaluationsExecuteAllExplicitTest.class,
		// Per-eval overrides top-level defaults (Section 7.1.1.1)
		AuthzenPDPEvaluationsPerEvalOverridesDefaultTest.class,
		// Missing subject everywhere returns decision-false evaluation (Cert Profile 3.4.1)
		AuthzenPDPEvaluationsMissingSubjectReturnsDecisionFalseTest.class,
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
		AuthzenPDPEvaluationsBatchWithDefaultValueMergingTest.class,
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
