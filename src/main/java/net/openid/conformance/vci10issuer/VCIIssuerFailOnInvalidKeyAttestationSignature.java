package net.openid.conformance.vci10issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIInvalidateKeyAttestationSignature;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

/**
 * Negative test that verifies the issuer properly rejects key attestations with invalid signatures.
 * This test invalidates the signature on the key attestation JWT and expects the issuer to
 * respond with an invalid_proof error.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-D.1">OID4VCI Appendix D.1 - Key Attestation in JWT format</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-key-attestation-signature",
	displayName = "OID4VCI 1.0: Issuer fail on invalid key attestation signature",
	summary = "This test case checks for proper error handling when a key attestation with an invalid signature is submitted. " +
		"The test sends a credential request with a key attestation JWT where the signature has been modified to be invalid. " +
		"The issuer must reject this request with an invalid_proof error. " +
		"Note: This test only runs when key attestation is required by the credential configuration. " +
		"If key attestation is not required, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidKeyAttestationSignature extends VCIIssuerHappyFlow {

	@Override
	public void start() {
		// Skip this test if the credential configuration doesn't require cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("This test requires a credential configuration with cryptographic binding (proof). " +
				"The selected credential configuration does not require proof, so key attestation signature validation cannot be tested.");
			return;
		}

		// Check if key attestation is required
		String proofTypeKey = env.getString("vci_proof_type_key");
		JsonObject proofType = env.getObject("vci_proof_type");
		boolean keyAttestationRequired = "attestation".equals(proofTypeKey) ||
			(proofType != null && proofType.has("key_attestations_required"));

		if (!keyAttestationRequired) {
			fireTestSkipped("This test requires key attestation. " +
				"The selected credential configuration uses '" + proofTypeKey + "' proof type without key_attestations_required.");
			return;
		}

		super.start();
	}

	@Override
	protected void afterKeyAttestationGeneration() {
		super.afterKeyAttestationGeneration();

		// Invalidate the key attestation signature
		callAndContinueOnFailure(VCIInvalidateKeyAttestationSignature.class, Condition.ConditionResult.INFO, "OID4VCI-1FINALA-D.1");
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		verifyCredentialIssuerCredentialErrorResponse();
		// Expect an error response when key attestation signature is invalid
		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_PROOF), "OID4VCI-1FINAL-8.3.1");
	}
}
