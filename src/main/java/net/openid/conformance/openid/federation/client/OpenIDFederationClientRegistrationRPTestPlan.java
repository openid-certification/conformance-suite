package net.openid.conformance.openid.federation.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-federation-entity-joined-to-test-federation-rp-test-plan",
	displayName = "OpenID Federation: Entity joined to test federation RP test (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.federationTest,
	testModules = {
		OpenIDFederationClientInvalidIssInEntityConfigurationTest.class,
		OpenIDFederationClientInvalidSubInEntityConfigurationTest.class,
		OpenIDFederationClientInvalidMissingExpInEntityConfigurationTest.class,
		OpenIDFederationClientInvalidMissingIatInEntityConfigurationTest.class,
		OpenIDFederationClientInvalidMissingClientRegistrationTypesSupportedTest.class,
		OpenIDFederationClientInvalidEmptyClientRegistrationTypesSupportedTest.class,
		OpenIDFederationClientTest.class,
		OpenIDFederationClientValidUnknownClientRegistrationTypesSupportedTest.class,
		OpenIDFederationClientInvalidAudInIdTokenTest.class,
		OpenIDFederationClientInvalidIssInIdTokenTest.class,
	}
)
public class OpenIDFederationClientRegistrationRPTestPlan implements TestPlan {

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("OpenID Federation: Entity joined to test federation RP");
	}

}
