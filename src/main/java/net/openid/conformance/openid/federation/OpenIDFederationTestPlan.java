package net.openid.conformance.openid.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-federation-test-plan",
	displayName = "OpenID Federation: Authorization server test (alpha - INCOMPLETE/INCORRECT, please email certification team if intererested)",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		OpenIDFederationEntityConfigurationVerificationTest.class,
		OpenIDFederationEntityMetadataVerificationTest.class
	}
)
public class OpenIDFederationTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID Federation OP";
	}

}
