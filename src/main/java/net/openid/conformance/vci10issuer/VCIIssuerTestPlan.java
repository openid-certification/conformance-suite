package net.openid.conformance.vci10issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-1_0-issuer-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final: Test an issuer (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciissuer,
	specFamily = TestPlan.SpecFamilyNames.oid4vci
)
public class VCIIssuerTestPlan implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			// Metadata tests are in a separate group because they extend AbstractVciTest (not
			// AbstractFAPI2SPFinalServerTestModule) and need fewer variants. The variant list is
			// intentionally empty: CI passes both fapi_profile=vci and fapi_profile=vci_haip
			// through this plan, so pinning a fixed value would cause "test plan already sets
			// this variant" errors. The @VariantNotApplicable annotations on the metadata test
			// classes restrict the profile to VCI values only.
			new ModuleListEntry(
				List.of(
					VCIIssuerMetadataTest.class,
					VCIIssuerMetadataSignedTest.class
				),
				List.of(
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
