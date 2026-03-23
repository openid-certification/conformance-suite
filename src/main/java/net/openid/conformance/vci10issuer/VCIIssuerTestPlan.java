package net.openid.conformance.vci10issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;

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
					new Variant(FAPI2FinalOPProfile.class, "vci"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response")
				)
			)
		);
	}
}
