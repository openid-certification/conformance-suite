package net.openid.conformance.vci10wallet;

import net.openid.conformance.fapi2spfinal.FAPI2MessageSigningFinalClientTestPlan;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientRefreshTokenTest;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmSignatureAlgIsNotNone;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithExpiredExpFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithInvalidAudFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithInvalidIssFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithInvalidSigFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithoutAudFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithoutExpFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestEnsureJarmWithoutIssFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidAlternateAlg;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidAud;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidExpiredExp;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidIss;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidMissingAud;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidMissingExp;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidMissingIss;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidMissingNonce;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidNonce;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidNullAlg;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidOpenBankingIntentId;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestInvalidSecondaryAud;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalClientTestValidAudAsArray;
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
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCICredentialIssuanceMode;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
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
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
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
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCICredentialIssuanceMode.class, "immediate"),
					new Variant(VCICredentialEncryption.class, "encrypted")
				)
			),
			// FAPI2SP client tests — same as FAPI2SPFinalClientTestPlan minus id_token /
			// JARM / OB-intent / Brazil-CBUAE-only refresh token modules. The wallet under
			// test is configured with the AS URL exposed by the suite; VCI-specific
			// metadata exposure (credential issuer metadata, credential / nonce / deferred
			// / notification endpoints) is exercised by the VCIWalletTest* modules above.
			new ModuleListEntry(
				vciFapi2SPFinalClientTestModules(),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(FAPIClientType.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(VCICredentialEncryption.class, "plain"),
					new Variant(VCICredentialIssuanceMode.class, "immediate")
				),
				// VCI variants are plan-level context — FAPI2SP modules don't declare them
				Set.of(VCIGrantType.class, VCICredentialEncryption.class, VCICredentialIssuanceMode.class,
					VCI1FinalCredentialFormat.class, VCICredentialOfferParameterVariant.class,
					VCIAuthorizationCodeFlowVariant.class)
			)
		);
	}

	public static List<Class<? extends TestModule>> vciFapi2SPFinalClientTestModules() {
		List<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2MessageSigningFinalClientTestPlan.testModules);

		// id_token / OIDC-only modules — VCI HAIP wallet is plain_oauth
		modules.remove(FAPI2SPFinalClientTestInvalidIss.class);
		modules.remove(FAPI2SPFinalClientTestInvalidAud.class);
		modules.remove(FAPI2SPFinalClientTestInvalidSecondaryAud.class);
		modules.remove(FAPI2SPFinalClientTestInvalidNullAlg.class);
		modules.remove(FAPI2SPFinalClientTestInvalidAlternateAlg.class);
		modules.remove(FAPI2SPFinalClientTestInvalidExpiredExp.class);
		modules.remove(FAPI2SPFinalClientTestInvalidMissingExp.class);
		modules.remove(FAPI2SPFinalClientTestInvalidMissingAud.class);
		modules.remove(FAPI2SPFinalClientTestInvalidMissingIss.class);
		modules.remove(FAPI2SPFinalClientTestValidAudAsArray.class);
		modules.remove(FAPI2SPFinalClientTestInvalidNonce.class);
		modules.remove(FAPI2SPFinalClientTestInvalidMissingNonce.class);

		// JARM modules — VCI HAIP wallet uses plain_response
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithoutIssFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithInvalidIssFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithoutAudFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithInvalidAudFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithoutExpFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithExpiredExpFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithInvalidSigFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmSignatureAlgIsNotNone.class);

		// Profile-incompatible
		modules.remove(FAPI2SPFinalClientTestInvalidOpenBankingIntentId.class); // openbanking_uk only
		modules.remove(FAPI2SPFinalClientRefreshTokenTest.class);               // openbanking_brazil / cbuae only

		return modules;
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
