package net.openid.conformance.vci10issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-1_0-issuer-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance draft 16: Test an issuer (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciissuer
)
public class VCIIssuerTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerMetadataTest.class,
					VCIIssuerHappyFlow.class,
					VCIIssuerEnsureServerAcceptsRequestObjectWithMultipleAud.class // may not be that useful but currently used for op-against-rp tests in our CI - maybe we should add a simple 'only one authorization' test in the test plan before the happy flow that uses two clients (as using two clients doesn't work with rp tests)
					// negative tests
				),
				List.of(
				)
			)
		);
	}
}
