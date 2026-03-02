package net.openid.conformance.vci10issuer;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

/**
 * Negative test: sends a credential request without the required proofs parameter.
 *
 * Per OID4VCI Section 9, "invalid_proof" error should be returned when:
 *   "the proofs parameter in the Credential Request is invalid: (1) if the field is missing"
 *
 * This test verifies the issuer correctly rejects a credential request that omits proofs
 * when the credential configuration requires cryptographic binding.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3.1">OID4VCI Section 8.3.1</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-missing-proof",
	displayName = "OID4VCI 1.0: Issuer fail on missing proof",
	summary = "This test verifies that the issuer rejects a credential request that omits the required 'proofs' parameter. " +
		"When the credential configuration requires cryptographic binding (proof), sending a request without proofs " +
		"should result in an 'invalid_proof' error response. " +
		"Note: This test requires a credential configuration that requires cryptographic binding (proof). " +
		"If the selected credential configuration does not require proof, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnMissingProof extends AbstractVCIIssuerTestModule {

	@Override
	public void start() {
		// Skip this test if the credential configuration doesn't require cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("This test requires a credential configuration with cryptographic binding (proof). " +
				"The selected credential configuration does not require proof, so missing proof rejection cannot be tested.");
			return;
		}

		super.start();
	}

	@Override
	protected void createCredentialRequest() {
		// Temporarily clear the cryptographic binding flag so VCICreateCredentialRequest
		// will not add proofs to the request
		env.putBoolean("vci_requires_cryptographic_binding", false);

		super.createCredentialRequest();

		// Restore the flag
		env.putBoolean("vci_requires_cryptographic_binding", true);
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		verifyCredentialIssuerCredentialErrorResponse();

		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_PROOF), "OID4VCI-1FINAL-8.3.1");
	}
}
