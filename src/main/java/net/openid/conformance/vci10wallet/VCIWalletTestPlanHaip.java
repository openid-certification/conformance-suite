package net.openid.conformance.vci10wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCICredentialIssuanceMode;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan (
	testPlanName = "oid4vci-1_0-wallet-haip-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.vciwallet
)
public class VCIWalletTestPlanHaip implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		List<Class<? extends TestModule>> testModules = new ArrayList<>(VCIWalletTestPlan.testModules);

		// Not needed for HAIP
		testModules.remove(VCIWalletTestCredentialIssuanceUsingScopesWithoutAuthorizationDetailsInTokenResponse.class);

		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIClientAuthType.class, "client_attestation"),
					new Variant(VCICredentialIssuanceMode.class, "immediate"),
					new Variant(VCICredentialEncryption.class, "plain")
				)
			),
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIClientAuthType.class, "client_attestation"),
					new Variant(VCICredentialIssuanceMode.class, "deferred"),
					new Variant(VCICredentialEncryption.class, "plain")
				)
			),
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIClientAuthType.class, "client_attestation"),
					new Variant(VCICredentialIssuanceMode.class, "immediate"),
					new Variant(VCICredentialEncryption.class, "encrypted")
				)
			)
		);
	}
}
