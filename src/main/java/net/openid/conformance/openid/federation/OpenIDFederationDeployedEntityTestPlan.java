package net.openid.conformance.openid.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "openid-federation-deployed-entity-test-plan",
	displayName = "OpenID Federation: Deployed federation entity test - alpha tests (not currently part of certification program - please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.federationTest,
	specFamily = TestPlan.SpecFamilyNames.federation,
	testModules = {
		OpenIDFederationEntityConfigurationTest.class,
		// OpenIDFederationListAndFetchTest.class, TODO: Figure out a way to do this test for large federations
		OpenIDFederationEnsureFetchWithInvalidSubFailsTest.class,
		OpenIDFederationEnsureFetchWithIssAsSubFailsTest.class,
		OpenIDFederationPreconfiguredKeysMatchTrustAnchorsKeysTest.class,
		OpenIDFederationCompareTrustChainToResolveTest.class,
	}
)
public class OpenIDFederationDeployedEntityTestPlan implements TestPlan {

}
