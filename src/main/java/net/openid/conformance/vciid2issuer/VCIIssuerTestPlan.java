package net.openid.conformance.vciid2issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-id2-issuer-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance ID2: Test an issuer - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.vciissuer
)
public class VCIIssuerTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerMetadataTest.class

					// negative tests
				),
				List.of(
				)
			)
		);
	}
}
