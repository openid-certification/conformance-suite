package net.openid.conformance.vci10issuer;

import net.openid.conformance.fapi2spfinal.FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalAustraliaConnectIdTestClaimsParameterIdTokenIdentityClaims;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalDiscoveryEndpointVerification;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureAuthorizationRequestWith64CharNonceSuccess;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureAuthorizationRequestWithLongNonce;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureAuthorizationRequestWithoutNonceSuccess;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureClientAssertionInTokenEndpoint;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureClientAssertionWithExpIs5MinutesInPastFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureClientAssertionWithNoSubFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureClientAssertionWithWrongAudFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureDifferentNonceInsideAndOutsideRequestObject;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureOtherScopeOrderSucceeds;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureServerHandlesNonMatchingIntentId;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureSignedClientAssertionWithRS256Fails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREndpointAsArrayAudienceFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREndpointAsAudienceFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbf8SecondsInTheFutureIsAccepted;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPARTokenEndpointAsAudienceFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalTestClaimsParameterIdentityClaims;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalTestEssentialAcrScaClaim;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalTestPlan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@PublishTestPlan(
	testPlanName = "oid4vci-1_0-issuer-haip-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test an issuer (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciissuer,
	specFamily = TestPlan.SpecFamilyNames.oid4vci
)
public class VCIIssuerTestPlanHaip implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerMetadataTest.class,
					VCIIssuerMetadataSignedTest.class
				),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(ClientAuthType.class, "client_attestation")
				)
			),
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerHappyFlow.class,
					VCIIssuerHappyFlowAdditionalRequests.class,
					VCIIssuerHappyFlowMultipleClients.class,
					VCIIssuerHappyFlowWithSkipNotification.class,
					// negative tests
					VCIIssuerFailOnInvalidNonce.class,
					VCIIssuerFailOnInvalidJwtProofSignature.class,
					VCIIssuerFailOnInvalidKeyAttestationSignature.class,
					VCIIssuerFailOnInvalidClientAttestationSignature.class,
					VCIIssuerFailOnInvalidClientAttestationPopSignature.class,
					VCIIssuerFailOnMismatchedClientAttestationPopKey.class,
					VCIIssuerFailOnMissingProof.class,
					VCIIssuerFailOnUnknownCredentialConfigurationId.class,
					VCIIssuerFailOnUnknownCredentialIdentifier.class,
					VCIIssuerFailOnRequestWithAccessTokenInQuery.class
				),
				List.of(
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCICredentialEncryption.class, "plain"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response")
				)
			),
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerHappyFlow.class,
					// negative tests
					VCIIssuerFailOnUnknownCredentialConfigurationId.class,
					VCIIssuerFailOnUnsupportedEncryptionAlgorithm.class
				),
				List.of(
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCICredentialEncryption.class, "encrypted"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response")
				)
			),
			// FAPI2 Security Profile tests - same as FAPI2SPFinalTestPlan minus signing-only,
			// profile-specific, OpenID-Connect-only, and private_key_jwt-only tests.
			// Discovery test is in its own entry as it doesn't have VCI variant parameters.
			new ModuleListEntry(
				List.of(
					FAPI2SPFinalDiscoveryEndpointVerification.class
				),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(AuthorizationRequestType.class, "simple")
				)
			),
			new ModuleListEntry(
				vciFapi2SPFinalTestModules(),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci_haip"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(VCICredentialEncryption.class, "plain")
				),
				// VCI variants are plan-level context — FAPI2SP modules don't declare them
				Set.of(VCIGrantType.class, VCICredentialEncryption.class, VCI1FinalCredentialFormat.class)
			)
		);
	}

	public static List<Class<? extends TestModule>> vciFapi2SPFinalTestModules() {
		List<Class<? extends TestModule>> fapiTestModules = new ArrayList<>(
			FAPI2SPFinalTestPlan.fapi2SPtestModules()
		);

		// Discovery test is in its own ModuleListEntry (no VCI variant parameters)
		fapiTestModules.remove(FAPI2SPFinalDiscoveryEndpointVerification.class);

		// Nonce / OpenID-Connect-only tests (VCI HAIP is plain_oauth)
		fapiTestModules.remove(FAPI2SPFinalEnsureAuthorizationRequestWithoutNonceSuccess.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureAuthorizationRequestWith64CharNonceSuccess.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureDifferentNonceInsideAndOutsideRequestObject.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureAuthorizationRequestWithLongNonce.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureOtherScopeOrderSucceeds.class);
		fapiTestModules.remove(FAPI2SPFinalTestClaimsParameterIdentityClaims.class);

		// private_key_jwt-only tests (VCI HAIP uses client_attestation)
		fapiTestModules.remove(FAPI2SPFinalEnsureSignedClientAssertionWithRS256Fails.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureClientAssertionInTokenEndpoint.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureClientAssertionWithExpIs5MinutesInPastFails.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureClientAssertionWithWrongAudFails.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureClientAssertionWithNoSubFails.class);
		fapiTestModules.remove(FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbf8SecondsInTheFutureIsAccepted.class);
		fapiTestModules.remove(FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails.class);
		fapiTestModules.remove(FAPI2SPFinalPAREndpointAsArrayAudienceFails.class);
		fapiTestModules.remove(FAPI2SPFinalPAREndpointAsAudienceFails.class);
		fapiTestModules.remove(FAPI2SPFinalPARTokenEndpointAsAudienceFails.class);

		// Profile-specific tests (plain_fapi / connectid_au / openbanking_uk / openbanking_brazil)
		fapiTestModules.remove(FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri.class);
		fapiTestModules.remove(FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails.class);
		fapiTestModules.remove(FAPI2SPFinalAustraliaConnectIdTestClaimsParameterIdTokenIdentityClaims.class);
		fapiTestModules.remove(FAPI2SPFinalEnsureServerHandlesNonMatchingIntentId.class);
		fapiTestModules.remove(FAPI2SPFinalTestEssentialAcrScaClaim.class);
		fapiTestModules.remove(FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails.class);

		return fapiTestModules;
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {
		String credentialFormat = variantSelection.getVariantParameterValue(VCI1FinalCredentialFormat.class);
		String codeFlowVariant = variantSelection.getVariantParameterValue(VCIAuthorizationCodeFlowVariant.class);
		return List.of(String.format("%s %s %s %s", "OID4VCI-1.0-FINAL+HAIP-1.0-FINAL", "Issuer", credentialFormat, codeFlowVariant));
	}
}
