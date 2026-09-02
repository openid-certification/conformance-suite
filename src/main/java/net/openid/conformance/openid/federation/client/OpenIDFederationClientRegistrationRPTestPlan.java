package net.openid.conformance.openid.federation.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "openid-federation-entity-joined-to-test-federation-rp-test-plan",
	displayName = "OpenID Federation: Entity joined to test federation RP test - alpha tests (not currently part of certification program - please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.federationTest,
	specFamily = TestPlan.SpecFamilyNames.federation,
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

}
