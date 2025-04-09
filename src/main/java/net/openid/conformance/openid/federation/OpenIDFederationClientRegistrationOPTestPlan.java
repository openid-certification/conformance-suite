package net.openid.conformance.openid.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-federation-client-registration-optest-plan",
	displayName = "OpenID Federation: Client Registration OP test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	profile = TestPlan.ProfileNames.federationTest,
	testModules = {
		OpenIDFederationEntityConfigurationTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndGetTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndPostTest.class,
		OpenIDFederationAutomaticClientRegistrationWithParTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndGetAndTrustChainTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndPostAndTrustChainTest.class,
		OpenIDFederationAutomaticClientRegistrationWithParAndTrustChainTest.class,
		OpenIDFederationAutomaticClientRegistrationInvalidClientIdInRequestObjectTest.class,
		OpenIDFederationAutomaticClientRegistrationInvalidClientIdInQueryParametersTest.class,
	}
)
public class OpenIDFederationClientRegistrationOPTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID Federation Client Registration OP";
	}

}
