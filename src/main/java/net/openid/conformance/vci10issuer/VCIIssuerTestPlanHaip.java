package net.openid.conformance.vci10issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

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
					new Variant(VCIProfile.class, "haip"),
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
					VCIIssuerFailOnReplayNonce.class,
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
					new Variant(VCIProfile.class, "haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCICredentialEncryption.class, "plain")
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
					new Variant(VCIProfile.class, "haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCICredentialEncryption.class, "encrypted")
				)
			)
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {
		String credentialFormat = variantSelection.getVariantParameterValue(VCI1FinalCredentialFormat.class);
		String codeFlowVariant = variantSelection.getVariantParameterValue(VCIAuthorizationCodeFlowVariant.class);
		return List.of(String.format("%s %s %s %s", "OID4VCI-1.0-FINAL+HAIP-1.0-FINAL", "Issuer", credentialFormat, codeFlowVariant));
	}
}
