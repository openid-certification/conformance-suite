package net.openid.conformance.authzen.scenarios.discovery;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

/**
 * The Discovery certification level (certification scenario 9): an independent level, not a
 * prerequisite for the API levels, that only checks the PDP metadata. The API plans also run
 * these checks under their discovery variant, but certify the API, not the metadata.
 */
@PublishTestPlan(
	testPlanName = "authzen-pdp-discovery-test-plan",
	shortName = "Discovery",
	certifiable = true,
	displayName = "AuthZEN 1.0: PDP server test for Discovery",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		AuthzenPDPDiscoveryTest.class
	}
)
public class AuthzenPDPDiscoveryTestPlan implements TestPlan {
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("AuthZEN Discovery");
	}
}
