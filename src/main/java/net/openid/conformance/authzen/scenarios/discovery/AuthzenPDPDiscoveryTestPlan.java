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
	summary = "Tests a PDP's metadata (Section 9) against the Discovery level of the [certification scenario](https://openid.github.io/authzen/authorization-api-1_0-certification-scenario.html): "
		+ "fetches the metadata from the well-known URL derived from the PDP Identifier (Section 9.2.1), and checks the response, the required and optional parameters, "
		+ "and signed_metadata when present against the 'PDP JWK Set' in the test configuration (Sections 9.1.3 and 9.2). No API endpoint is called. "
		+ "This level is independent of the API levels: the API plans run the same checks when 'PDP server metadata location' is 'discovery', but certify the API, not the metadata.",
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
