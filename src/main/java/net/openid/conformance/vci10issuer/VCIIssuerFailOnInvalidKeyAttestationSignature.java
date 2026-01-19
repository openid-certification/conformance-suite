package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIInvalidateKeyAttestationSignature;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialErrorResponse;
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
	displayName = "OID4VCI 1.0: Issuer with invalid key attestation signature",
	summary = "This test case checks for proper error handling when a key attestation with an invalid signature is submitted. " +
		"The test sends a credential request with a key attestation JWT where the signature has been modified to be invalid. " +
		"The issuer must reject this request with an invalid_proof error. " +
		"Note: This test only runs when key attestation is required by the credential configuration. " +
		"If key attestation is not required, the test behaves like the happy flow.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidKeyAttestationSignature extends VCIIssuerHappyFlow {

	private boolean keyAttestationWasGenerated = false;

	@Override
	protected void afterKeyAttestationGeneration() {
		super.afterKeyAttestationGeneration();

		// Check if a key attestation was actually generated
		String keyAttestationJwt = env.getString("key_attestation_jwt");
		if (keyAttestationJwt != null && !keyAttestationJwt.isEmpty()) {
			keyAttestationWasGenerated = true;
			callAndContinueOnFailure(VCIInvalidateKeyAttestationSignature.class, Condition.ConditionResult.INFO, "OID4VCI-1FINALA-D.1");
		}
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		if (keyAttestationWasGenerated) {
			// Expect an error response when key attestation signature is invalid
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3.1");

			callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialErrorResponse.class, Condition.ConditionResult.WARNING, "OID4VCI-1FINAL-8.3.1");
			callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_PROOF), "OID4VCI-1FINAL-8.3.1");
		} else {
			// No key attestation was required, run normal verification
			super.verifyCredentialIssuerCredentialResponse();
		}
	}
}
