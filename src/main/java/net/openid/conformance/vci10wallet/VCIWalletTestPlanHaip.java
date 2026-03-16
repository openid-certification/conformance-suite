package net.openid.conformance.vci10wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCICredentialIssuanceMode;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.vci10issuer.VCI1FinalCredentialFormat;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan (
	testPlanName = "oid4vci-1_0-wallet-haip-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.vciwallet,
	specFamily = TestPlan.SpecFamilyNames.oid4vci
)
public class VCIWalletTestPlanHaip implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

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
					new Variant(ClientAuthType.class, "client_attestation"),
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
					new Variant(ClientAuthType.class, "client_attestation"),
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
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCICredentialIssuanceMode.class, "immediate"),
					new Variant(VCICredentialEncryption.class, "encrypted")
				)
			)
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {
		String credentialFormat = variantSelection.getVariantParameterValue(VCI1FinalCredentialFormat.class);
		String codeFlowVariant = variantSelection.getVariantParameterValue(VCIAuthorizationCodeFlowVariant.class);
		String credentialOfferVariant = variantSelection.getVariantParameterValue(VCICredentialOfferParameterVariant.class);

		if (credentialOfferVariant != null) {
			return List.of(String.format("%s %s %s %s %s", "OID4VCI-1.0-FINAL+HAIP-1.0-FINAL", "Wallet", credentialFormat, codeFlowVariant, credentialOfferVariant));
		}
		return List.of(String.format("%s %s %s %s", "OID4VCI-1.0-FINAL+HAIP-1.0-FINAL", "Wallet", credentialFormat, codeFlowVariant));
	}
}
