package net.openid.conformance.openid.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-federation-client-registration-optest-plan",
	displayName = "OpenID Federation: Client Registration OP test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	profile = TestPlan.ProfileNames.federationTest,
	testModules = {
	}
)
public class OpenIDFederationClientRegistrationRPTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID Federation Client Registration OP";
	}

}
