package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-resource-search-test-plan",
	displayName = "Authzen 1.0: PDP server test for Resource Search - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Resource Search API tests from https://github.com/openid/authzen/issues/433
		// Search Core
		AuthzenPDPResourceSearchValidResourceSearchRequestWithNonEmptyResultsTest.class,
		AuthzenPDPResourceSearchResourceSearchWithResourceIdOmittedTest.class,
		// X-Request-ID handling (Section 10.1.3)
		AuthzenPDPResourceSearchXRequestIdEchoedTest.class,
		// Idempotency
		AuthzenPDPResourceSearchIdempotencyTest.class,
		// Transport binding negative tests (Section 10.1.1 / 10.1.2)
		AuthzenPDPResourceSearchRejectGetMethodTest.class,
		AuthzenPDPResourceSearchRejectPutMethodTest.class,
		AuthzenPDPResourceSearchRejectTopLevelArrayTest.class,
		AuthzenPDPResourceSearchRejectNonJsonContentTypeTest.class,
		AuthzenPDPResourceSearchAcceptContentTypeWithCharsetTest.class,
		AuthzenPDPResourceSearchRejectMalformedJsonTest.class,
		AuthzenPDPResourceSearchRejectEmptyBodyTest.class,
		// Error handling: HTTP 400 negative tests (Section 10.1)
		AuthzenPDPResourceSearchMissingSubjectTest.class,
		AuthzenPDPResourceSearchSubjectMissingIdTest.class,
		AuthzenPDPResourceSearchMissingResourceTest.class,
		AuthzenPDPResourceSearchResourceMissingTypeTest.class,
		AuthzenPDPResourceSearchMissingActionTest.class,
		AuthzenPDPResourceSearchActionMissingNameTest.class,
		// Extra resource.id ignored (Section 8.5.1)
		AuthzenPDPResourceSearchExtraResourceIdIgnoredTest.class,
		// Search Properties (Properties variant only)
		AuthzenPDPResourceSearchResourceSearchWithSubjectPropertiesTest.class,
	}
)
public class AuthzenPDPResourceSearchTestPlan implements TestPlan {
}
