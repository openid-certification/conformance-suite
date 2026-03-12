package net.openid.conformance.openid.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-test-3rd-party-init-login-test-plan",
	displayName = "OpenID Connect Core Client Login Tests: Relying party 3rd party initiated login tests",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		OIDCCClient3rdPartyInitiatedLoginTest.class
	}
)
public class OIDCCClient3rdPartyInitiatedLoginTestPlan implements TestPlan {

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("3rd Party-Init RP");
	}

}
