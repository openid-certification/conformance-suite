package net.openid.conformance.vciid2wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan (
	testPlanName = "oid4vci-id2-wallet-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance ID2: Test a wallet (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciwallet
)
public class VCIWalletTestPlan implements TestPlan {


	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					VCIWalletHappyPath.class
				),
				List.of(
				)
			)
		);
	}
}
