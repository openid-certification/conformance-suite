package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

/**
 * Superset of {@link AuthzenPDPEvaluationsTestPlan} that additionally exercises the
 * OPTIONAL parts of the Access Evaluations API.
 *
 * <p>Section 7.1.2.1 defines three evaluation semantics, but {@code options} — and with it
 * {@code evaluations_semantic} — is an OPTIONAL request member, so a conformant PDP may
 * implement only the {@code execute_all} default. The modules below
 * therefore cannot sit in a plan a PDP is expected to pass in full:
 *
 * <ul>
 *   <li>{@link AuthzenPDPEvaluationsDenyOnFirstDenyTest} and
 *       {@link AuthzenPDPEvaluationsPermitOnFirstPermitTest} require the short circuit to be
 *       observable on the wire (a truncated {@code evaluations} array), which a PDP that does
 *       not implement the option cannot produce;</li>
 *   <li>{@link AuthzenPDPEvaluationsDenyOnFirstDenyWithoutDenialTest} and
 *       {@link AuthzenPDPEvaluationsPermitOnFirstPermitWithoutPermitTest} send a batch that never
 *       reaches the trigger, so every decision must come back. A PDP that ignores the option
 *       passes them too; they catch one that implements it but drops results it should
 *       return;</li>
 *   <li>{@link AuthzenPDPEvaluationsUnknownSemanticValueTest} requires HTTP 400 for an
 *       unrecognized {@code evaluations_semantic} value, which presupposes the option is
 *       implemented at all — a PDP that ignores {@code options} answers 200.</li>
 * </ul>
 *
 * <p>Run this plan to check a PDP that does claim support for the short-circuiting semantics;
 * run {@link AuthzenPDPEvaluationsTestPlan} for the certification-track subset. Because the
 * plan requires OPTIONAL features, it returns no certification profile name, so the suite
 * does not build a certification package for it.
 *
 * <p>The module list is a superset of {@link AuthzenPDPEvaluationsTestPlan}'s and must stay
 * one — {@code AuthzenPDPEvaluationsComprehensiveTestPlan_UnitTest} fails the build if a
 * module is added to that plan without being added here.
 */
@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluations-comprehensive-test-plan",
	shortName = "Comprehensive",
	certifiable = false,
	displayName = "AuthZEN 1.0: PDP server test for batch evaluations, including optional evaluation semantics (not part of certification program - use the batch evaluations plan to certify)",
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
		// evaluations_semantic option (Section 7.1.2.1), including the OPTIONAL
		// short-circuiting semantics that AuthzenPDPEvaluationsTestPlan leaves out
		AuthzenPDPEvaluationsExecuteAllExplicitTest.class,
		AuthzenPDPEvaluationsDenyOnFirstDenyTest.class,
		AuthzenPDPEvaluationsPermitOnFirstPermitTest.class,
		AuthzenPDPEvaluationsDenyOnFirstDenyWithoutDenialTest.class,
		AuthzenPDPEvaluationsPermitOnFirstPermitWithoutPermitTest.class,
		AuthzenPDPEvaluationsUnknownSemanticValueTest.class,
		// Per-eval overrides top-level defaults (Section 7.1.1)
		AuthzenPDPEvaluationsPerEvalOverridesDefaultTest.class,
		// No subject at any level: 400, or a decision-false evaluation (Section 7.1, 7.1.2.1)
		AuthzenPDPEvaluationsMissingSubjectTest.class,
		// Error handling at every level (certification scenario 3.1 with 5.4.2 and 5.4.6)
		AuthzenPDPEvaluationsSubjectMissingTypeTest.class,
		AuthzenPDPEvaluationsSubjectIsStringTest.class,
		AuthzenPDPEvaluationsContextIsStringTest.class,
		AuthzenPDPEvaluationsEvaluationsIsObjectTest.class,
		AuthzenPDPEvaluationsEvaluationIsStringTest.class,
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
		AuthzenPDPEvaluationsRejectMissingContentTypeTest.class,
		AuthzenPDPEvaluationsAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPEvaluationsRejectMalformedJsonTest.class,
		AuthzenPDPEvaluationsRejectEmptyBodyTest.class,
		// Invalid field type: certification scenario 5.4.6 requires HTTP 400 "when a field has an
		// invalid type". options is OPTIONAL for a PDP to act on, but every Batch Core PDP accepts
		// it (execute-all-explicit above), so it is checked like context and properties.
		AuthzenPDPEvaluationsOptionsIsStringTest.class,
		// Batch Properties (Properties variant only)
		AuthzenPDPEvaluationsBatchWithPropertiesValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithSubjectPropertiesValidatedTest.class,
		AuthzenPDPEvaluationsBatchWithTopLevelDefaultInheritanceTest.class,
		AuthzenPDPEvaluationsBatchOverrideReplacesDefaultEntityTest.class,
		// Authentication (Sections 11.2 and 11.3)
		AuthzenPDPEvaluationsMissingCredentialsReturns401Test.class,
		AuthzenPDPEvaluationsUnauthenticatedRequestAcceptedTest.class,
		AuthzenPDPEvaluationsInvalidCredentialsReturns401Test.class,
	}
)
public class AuthzenPDPEvaluationsComprehensiveTestPlan implements TestPlan {
}
