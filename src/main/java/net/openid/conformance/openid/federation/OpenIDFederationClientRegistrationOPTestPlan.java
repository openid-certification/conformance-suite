package net.openid.conformance.openid.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-federation-entity-joined-to-test-federation-op-test-plan",
	displayName = "OpenID Federation: Entity joined to test federation OP test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	profile = TestPlan.ProfileNames.federationTest,
	testModules = {
		OpenIDFederationEntityConfigurationTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndGetTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndPostTest.class,
		OpenIDFederationAutomaticClientRegistrationWithParTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndGetAndTrustChainTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndPostAndTrustChainTest.class,
		OpenIDFederationAutomaticClientRegistrationWithParAndTrustChainTest.class,
		OpenIDFederationAutomaticClientRegistrationWithJarAndEncryptedRequestObjectTest.class,
		OpenIDFederationAutomaticClientRegistrationInvalidClientIdInRequestObjectTest.class,
		OpenIDFederationAutomaticClientRegistrationInvalidClientIdInQueryParametersTest.class,
		OpenIDFederationAutomaticClientRegistrationInvalidSignatureOnRequestObjectTest.class,
		OpenIDFederationAutomaticClientRegistrationInvalidSignatureWithOtherKeysOnRequestObjectTest.class
	}
)
public class OpenIDFederationClientRegistrationOPTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID Federation: Entity joined to test federation OP";
	}

}
