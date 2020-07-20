package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.OIDCCClient3rdPartyInitiatedLoginTest;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-client-test-3rd-party-init-login-test-plan",
	displayName = "OpenID Connect Core Client Login Tests: Relying party 3rd party initiated login tests",
	profile = TestPlan.ProfileNames.rplogouttest,
	testModules = {
		OIDCCClient3rdPartyInitiatedLoginTest.class
	}
)
public class OIDCCClient3rdPartyInitiatedLoginTestPlan implements TestPlan {
}
