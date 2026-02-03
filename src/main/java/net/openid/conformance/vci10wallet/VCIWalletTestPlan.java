package net.openid.conformance.vci10wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;

import java.util.List;

@PublishTestPlan (
	testPlanName = "oid4vci-1_0-wallet-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.vciwallet
)
public class VCIWalletTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		VCIWalletHappyPath.class,
		VCIWalletHappyPathUsingScopes.class,
		VCIWalletHappyPathUsingScopesWithoutAuthorizationDetailsInTokenResponse.class
	);

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
				)
			)
		);
	}
}
