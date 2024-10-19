package net.openid.conformance.openid.federation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-federation-test-plan",
	displayName = "OpenID Federation: Federation entity test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	profile = TestPlan.ProfileNames.federationTest,
	testModules = {
		OpenIDFederationEntityConfigurationTest.class,
		OpenIDFederationListAndFetchTest.class,
		OpenIDFederationEnsureFetchWithInvalidSubFailsTest.class,
		OpenIDFederationEnsureFetchWithIssAsSubFailsTest.class,
		OpenIDFederationPreconfiguredKeysMatchTrustAnchorsKeysTest.class,
		OpenIDFederationCompareTrustChainToResolveTest.class,
	}
)
public class OpenIDFederationTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID Federation entity";
	}

}
