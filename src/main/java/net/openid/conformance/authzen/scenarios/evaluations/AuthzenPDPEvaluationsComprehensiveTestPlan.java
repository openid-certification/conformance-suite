package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

/**
 * Superset of {@link AuthzenPDPEvaluationsTestPlan} that additionally exercises the
 * OPTIONAL parts of the Access Evaluations API.
 *
 * <p>Section 7.1.2.1 defines three evaluation semantics, but {@code options} — and with it
 * {@code evaluations_semantic} — is an OPTIONAL request member, so a conformant PDP may
 * implement only the {@code execute_all} default. The three short-circuiting modules below
 * therefore cannot sit in a plan a PDP is expected to pass in full:
 *
 * <ul>
 *   <li>{@link AuthzenPDPEvaluationsDenyOnFirstDenyTest} and
 *       {@link AuthzenPDPEvaluationsPermitOnFirstPermitTest} require the short circuit to be
 *       observable on the wire (a truncated {@code evaluations} array), which a PDP that does
 *       not implement the option cannot produce;</li>
 *   <li>{@link AuthzenPDPEvaluationsUnknownSemanticValueTest} requires HTTP 400 for an
 *       unrecognized value, which presupposes the option is implemented at all — a PDP that
 *       ignores {@code options} answers 200.</li>
 * </ul>
 *
 * <p>Run this plan to check a PDP that does claim support for the short-circuiting semantics;
 * run {@link AuthzenPDPEvaluationsTestPlan} for the certification-track subset.
 *
 * <p>The module list is a superset of {@link AuthzenPDPEvaluationsTestPlan}'s and must stay
 * one — {@code AuthzenPDPEvaluationsComprehensiveTestPlan_UnitTest} fails the build if a
 * module is added to that plan without being added here.
 */
@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluations-comprehensive-test-plan",
	displayName = "Comprehensive AuthZEN PDP server test (Not part of certification program)",
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
		// evaluations_semantic option (Section 7.1.2.1), including the OPTIONAL
		// short-circuiting semantics that AuthzenPDPEvaluationsTestPlan leaves out
		AuthzenPDPEvaluationsExecuteAllExplicitTest.class,
		AuthzenPDPEvaluationsDenyOnFirstDenyTest.class,
		AuthzenPDPEvaluationsPermitOnFirstPermitTest.class,
		AuthzenPDPEvaluationsUnknownSemanticValueTest.class,
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
public class AuthzenPDPEvaluationsComprehensiveTestPlan implements TestPlan {
}
