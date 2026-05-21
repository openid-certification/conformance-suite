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
		// Error handling: HTTP 400 negative tests (Section 4.7)
		AuthzenPDPSubjectSearchMissingActionTest.class,
		AuthzenPDPSubjectSearchResourceMissingIdTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPSubjectSearchSubjectSearchWithResourcePropertiesTest.class,
	}
)
public class AuthzenPDPSubjectSearchTestPlan implements TestPlan {
}
