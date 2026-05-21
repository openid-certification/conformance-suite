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
		// Search Properties (Properties variant only)
		AuthzenPDPActionSearchActionSearchWithPropertiesTest.class,
	}
)
public class AuthzenPDPActionSearchTestPlan implements TestPlan {
}
