package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIInvalidateJwtProofSignature;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

/**
 * Negative test that verifies the issuer properly rejects JWT proofs with invalid signatures.
 * This test invalidates the signature on the JWT proof and expects the issuer to
 * respond with an invalid_proof error.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-7.2.1">OID4VCI Section 7.2.1 - JWT Proof Type</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-jwt-proof-signature",
	displayName = "OID4VCI 1.0: Issuer with invalid JWT proof signature",
	summary = "This test case checks for proper error handling when a JWT proof with an invalid signature is submitted. " +
		"The test sends a credential request with a JWT proof where the signature has been modified to be invalid. " +
		"The issuer must reject this request with an invalid_proof error. " +
		"Note: This test only applies when using jwt proof type. For attestation proof type, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidJwtProofSignature extends VCIIssuerHappyFlow {

	@Override
	public void start() {
		// Skip this test if the credential configuration doesn't require cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("This test requires a credential configuration with cryptographic binding (proof). " +
				"The selected credential configuration does not require proof, so JWT proof signature validation cannot be tested.");
			return;
		}

		// Check if the proof type is jwt
		String proofTypeKey = env.getString("vci_proof_type_key");
		if (!"jwt".equals(proofTypeKey)) {
			fireTestSkipped("This test requires jwt proof type. " +
				"The selected credential configuration uses '" + proofTypeKey + "' proof type.");
			return;
		}

		super.start();
	}

	@Override
	protected void afterProofGeneration() {
		super.afterProofGeneration();

		// Invalidate the JWT proof signature
		callAndContinueOnFailure(VCIInvalidateJwtProofSignature.class, Condition.ConditionResult.INFO, "OID4VCI-1FINAL-7.2.1");
	}

	@Override
	protected void verifyEffectiveCredentialResponse() {
		// Expect an error response when JWT proof signature is invalid
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3.1");

		callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialErrorResponse.class, Condition.ConditionResult.WARNING, "OID4VCI-1FINAL-8.3.1");
		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_PROOF), "OID4VCI-1FINAL-8.3.1");
	}
}
