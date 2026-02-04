package net.openid.conformance.vci10issuer;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-1_0-issuer-haip-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test an issuer (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciissuer
)
public class VCIIssuerTestPlanHaip implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerMetadataTest.class,
					VCIIssuerMetadataSingedTest.class
				),
				List.of(
					new Variant(VCIProfile.class, "haip"),
					new Variant(VCIClientAuthType.class, "client_attestation")
				)
			),
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerHappyFlow.class,
					VCIIssuerEnsureServerAcceptsRequestObjectWithMultipleAud.class, // may not be that useful but currently used for op-against-rp tests in our CI - maybe we should add a simple 'only one authorization' test in the test plan before the happy flow that uses two clients (as using two clients doesn't work with rp tests)
					// negative tests
					VCIIssuerFailOnInvalidNonce.class,
					VCIIssuerFailOnReplayNonce.class,
					VCIIssuerFailOnInvalidJwtProofSignature.class,
					VCIIssuerFailOnInvalidKeyAttestationSignature.class,
					VCIIssuerFailOnInvalidClientAttestationSignature.class,
					VCIIssuerFailOnInvalidClientAttestationPopSignature.class,
					VCIIssuerFailOnUnsupportedEncryptionAlgorithm.class,
					VCIIssuerFailOnUnknownCredentialConfigurationId.class,
					VCIIssuerFailOnUnknownCredentialIdentifier.class
				),
				List.of(
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(VCIClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple")
				)
			)
		);
	}
}
