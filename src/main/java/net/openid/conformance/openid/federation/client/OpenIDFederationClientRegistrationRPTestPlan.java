package net.openid.conformance.openid.federation.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-federation-entity-joined-to-test-federation-rp-test-plan",
	displayName = "OpenID Federation: Entity joined to test federation RP test (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.federationTest,
	requireClientLog = true,
	testModules = {
		OpenIDFederationClientHappyPathTest.class,
	}
)
public class OpenIDFederationClientRegistrationRPTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID Federation: Entity joined to test federation RP";
	}

}
