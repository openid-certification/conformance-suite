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
		AuthzenPDPSubjectSearchUnknownEntityTypeTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPSubjectSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPSubjectSearchIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
		AuthzenPDPSubjectSearchRejectGetMethodTest.class,
		AuthzenPDPSubjectSearchRejectPutMethodTest.class,
		AuthzenPDPSubjectSearchRejectTopLevelArrayTest.class,
		AuthzenPDPSubjectSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPSubjectSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPSubjectSearchRejectMalformedJsonTest.class,
		AuthzenPDPSubjectSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 10.1)
		AuthzenPDPSubjectSearchMissingActionTest.class,
		AuthzenPDPSubjectSearchActionMissingNameTest.class,
		AuthzenPDPSubjectSearchActionIsStringTest.class,
		AuthzenPDPSubjectSearchMissingResourceTest.class,
		AuthzenPDPSubjectSearchResourceMissingIdTest.class,
		AuthzenPDPSubjectSearchResourceMissingTypeTest.class,
		AuthzenPDPSubjectSearchSubjectMissingTypeTest.class,
		// Forward-compat: unknown fields ignored (Section 10.1.1)
		AuthzenPDPSubjectSearchUnknownPageFieldsTest.class,
		// Extra subject.id ignored (Section 8.4.1)
		AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPSubjectSearchSubjectSearchWithResourcePropertiesTest.class,
	}
)
public class AuthzenPDPSubjectSearchTestPlan implements TestPlan {
}
