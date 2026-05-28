package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-subject-search-test-plan",
	displayName = "Authzen 1.0: PDP server test for Subject Search - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Subject Search API tests from https://github.com/openid/authzen/issues/433
		// Search Core
		AuthzenPDPSubjectSearchValidSubjectSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPSubjectSearchSubjectSearchWithContextTest.class,
		AuthzenPDPSubjectSearchSubjectSearchWithSubjectIdOmittedTest.class,
		AuthzenPDPSubjectSearchRequestWithPageLimitTest.class,
		AuthzenPDPSubjectSearchPaginatedSubjectSearchTest.class,
		AuthzenPDPSubjectSearchUnknownEntityTypeTest.class,
		// X-Request-ID handling (Spec 10.1.3-4)
		AuthzenPDPSubjectSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPSubjectSearchIdempotencyTest.class,
		// Transport binding negative tests (Spec 10.1.1 / 10.1.2 / 2.4.4 / 2.4.5)
		AuthzenPDPSubjectSearchRejectGetMethodTest.class,
		AuthzenPDPSubjectSearchRejectPutMethodTest.class,
		AuthzenPDPSubjectSearchRejectTopLevelArrayTest.class,
		AuthzenPDPSubjectSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPSubjectSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPSubjectSearchRejectMalformedJsonTest.class,
		AuthzenPDPSubjectSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 4.7)
		AuthzenPDPSubjectSearchMissingActionTest.class,
		AuthzenPDPSubjectSearchResourceMissingIdTest.class,
		// Forward-compat: unknown fields ignored (Spec 10.1.1-3)
		AuthzenPDPSubjectSearchUnknownPageFieldsTest.class,
		// Extra subject.id ignored (Spec 8.4.1-1)
		AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPSubjectSearchSubjectSearchWithResourcePropertiesTest.class,
	}
)
public class AuthzenPDPSubjectSearchTestPlan implements TestPlan {
}
