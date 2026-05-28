package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-action-search-test-plan",
	displayName = "Authzen 1.0: PDP server test for Action Search - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Action Search API tests from https://github.com/openid/authzen/issues/433
		// Search Core
		AuthzenPDPActionSearchValidActionSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPActionSearchActionSearchWithContextTest.class,
		AuthzenPDPActionSearchUnknownEntityIdentifierTest.class,
		// X-Request-ID handling (Spec 10.1.3-4)
		AuthzenPDPActionSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPActionSearchIdempotencyTest.class,
		// Transport binding negative tests (Spec 10.1.1 / 10.1.2 / 2.4.4 / 2.4.5)
		AuthzenPDPActionSearchRejectGetMethodTest.class,
		AuthzenPDPActionSearchRejectPutMethodTest.class,
		AuthzenPDPActionSearchRejectTopLevelArrayTest.class,
		AuthzenPDPActionSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPActionSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPActionSearchRejectMalformedJsonTest.class,
		AuthzenPDPActionSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 4.7)
		AuthzenPDPActionSearchMissingResourceTest.class,
		AuthzenPDPActionSearchResourceMissingIdTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPActionSearchActionSearchWithPropertiesTest.class,
	}
)
public class AuthzenPDPActionSearchTestPlan implements TestPlan {
}
