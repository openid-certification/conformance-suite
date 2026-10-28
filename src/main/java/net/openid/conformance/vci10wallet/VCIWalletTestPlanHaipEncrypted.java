package net.openid.conformance.vci10wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCICredentialIssuanceMode;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIWalletAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The HAIP wallet plan with credential request and response encryption in every module, for
 * wallets that only accept an encrypted credential response. OpenID4VCI 1.0 lets a wallet choose
 * to encrypt when the issuer supports it, and HAIP 1.0 says nothing about credential encryption,
 * so such a wallet is conformant, but it cannot run the plain modules of
 * {@link VCIWalletTestPlanHaip}. The issuer still advertises encryption with
 * {@code encryption_required} false.
 */
@PublishTestPlan (
	testPlanName = "oid4vci-1_0-wallet-haip-encrypted-test-plan",
	shortName = "1.0 Final + HAIP, encrypted",
	certifiable = false,
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test a wallet that requires credential encryption (Not part of certification program)",
	profile = TestPlan.ProfileNames.vciwallet,
	specFamily = TestPlan.SpecFamilyNames.oid4vci
)
public class VCIWalletTestPlanHaipEncrypted implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		List<Class<? extends TestModule>> testModules = new ArrayList<>(VCIWalletTestPlan.testModules);

		// Not needed for HAIP
		testModules.remove(VCIWalletTestCredentialIssuanceUsingScopesWithoutAuthorizationDetailsInTokenResponse.class);

		return List.of(
			new ModuleListEntry(testModules, vciVariants("immediate")),
			new ModuleListEntry(testModules, vciVariants("deferred")),
			new ModuleListEntry(
				VCIWalletTestPlanHaip.vciFapi2SPFinalClientTestModules(),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(FAPIClientType.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(VCICredentialEncryption.class, "encrypted"),
					new Variant(VCICredentialIssuanceMode.class, "immediate")
				),
				// VCI variants are plan-level context — FAPI2SP modules don't declare them
				Set.of(VCIGrantType.class, VCICredentialEncryption.class, VCICredentialIssuanceMode.class,
					VCI1FinalCredentialFormat.class, VCICredentialOfferParameterVariant.class,
					VCIWalletAuthorizationCodeFlowVariant.class)
			)
		);
	}

	private static List<Variant> vciVariants(String issuanceMode) {
		return List.of(
			new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
			new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
			new Variant(VCIGrantType.class, "authorization_code"),
			new Variant(AuthorizationRequestType.class, "simple"),
			new Variant(ClientAuthType.class, "client_attestation"),
			new Variant(FAPIClientType.class, "plain_oauth"),
			new Variant(FAPIResponseMode.class, "plain_response"),
			new Variant(VCICredentialIssuanceMode.class, issuanceMode),
			new Variant(VCICredentialEncryption.class, "encrypted")
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {
		return new VCIWalletTestPlanHaip().certificationProfileName(variantSelection).stream()
			.map(name -> name + " encrypted")
			.toList();
	}
}
