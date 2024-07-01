package net.openid.conformance.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "openid-federation-test-plan",
	displayName = "OPENID-Federation-Draft: Federation test",
	profile = TestPlan.ProfileNames.opfedtest,
	testModules = {
		FederationModule.class
	}
)
public class FederationTestPlan implements TestPlan {
}
