package net.openid.conformance.vci10issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI2FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-1_0-issuer-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final: Test an issuer (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciissuer
)
public class VCIIssuerTestPlan implements TestPlan {

	@Override
	public List<Variant> variantsNotApplicable() {
		return List.of(
			new Variant(FAPI2FinalOPProfile.class, "plain_fapi"),
			new Variant(FAPI2FinalOPProfile.class, "openbanking_uk"),
			new Variant(FAPI2FinalOPProfile.class, "consumerdataright_au"),
			new Variant(FAPI2FinalOPProfile.class, "openbanking_brazil"),
			new Variant(FAPI2FinalOPProfile.class, "connectid_au"),
			new Variant(FAPI2FinalOPProfile.class, "cbuae"),
			new Variant(FAPI2FinalOPProfile.class, "fapi_client_credentials_grant"),
			new Variant(FAPI2FinalOPProfile.class, "vci_haip")
		);
	}

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerMetadataTest.class,
					VCIIssuerMetadataSignedTest.class,
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
					VCIIssuerFailOnUnsupportedEncryptionAlgorithm.class,
					VCIIssuerFailOnUnknownCredentialConfigurationId.class,
					VCIIssuerFailOnUnknownCredentialIdentifier.class,
					VCIIssuerFailOnRequestWithAccessTokenInQuery.class
				),
				List.of(
				)
			)
		);
	}
}
