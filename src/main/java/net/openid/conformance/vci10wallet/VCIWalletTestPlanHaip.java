package net.openid.conformance.vci10wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCIProfile;

import java.util.List;

@PublishTestPlan (
	testPlanName = "oid4vci-1_0-wallet-haip-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.vciwallet
)
public class VCIWalletTestPlanHaip implements TestPlan {


	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					VCIWalletHappyPath.class,
					VCIWalletHappyPathUsingScopes.class,
					VCIWalletHappyPathUsingScopesWithoutAuthorizationDetailsInTokenResponse.class
				),
				List.of(
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip")
				)
			)
		);
	}
}
