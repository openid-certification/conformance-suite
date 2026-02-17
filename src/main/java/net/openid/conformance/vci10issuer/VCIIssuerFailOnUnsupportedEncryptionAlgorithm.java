package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.vci10issuer.condition.VCIUseUnsupportedEncryptionAlgorithm;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

/**
 * Negative test that verifies the issuer properly rejects credential requests with
 * unsupported encryption algorithms.
 *
 * This test modifies the credential_response_encryption.alg parameter to use an
 * unsupported value and expects the issuer to respond with an
 * invalid_encryption_parameters error.
 *
 * Note: This test only runs when vci_credential_encryption=encrypted is selected.
 * If encryption is disabled (plain), the test skips.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3.1">OID4VCI Section 8.3.1 - Credential Error Response</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-unsupported-encryption-algorithm",
	displayName = "OID4VCI 1.0: Issuer fail on unsupported encryption algorithm",
	summary = "This test case checks for proper error handling when a credential request contains " +
		"unsupported encryption parameters. The test sends a credential request with an invalid " +
		"encryption algorithm in the credential_response_encryption parameter. " +
		"The issuer must reject this request with an invalid_encryption_parameters error. " +
		"Note: This test requires vci_credential_encryption=encrypted variant. " +
		"If encryption is not enabled, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnUnsupportedEncryptionAlgorithm extends VCIIssuerHappyFlow {

	@Override
	public void start() {
		// Skip this test if encryption is not enabled
		if (vciCredentialEncryption != VCICredentialEncryption.ENCRYPTED) {
			fireTestSkipped("This test requires vci_credential_encryption=encrypted variant. " +
				"Skipping because encryption is not enabled.");
			return;
		}

		super.start();
	}

	@Override
	protected void afterCredentialResponseEncryptionAdded() {
		super.afterCredentialResponseEncryptionAdded();

		// Replace the encryption algorithm with an unsupported one
		callAndContinueOnFailure(VCIUseUnsupportedEncryptionAlgorithm.class, Condition.ConditionResult.INFO, "OID4VCI-1FINAL-11.2.3");
	}

	@Override
	protected void verifyEffectiveCredentialResponse() {
		super.verifyCredentialIssuerCredentialErrorResponse();

		// Expect an error response when encryption algorithm is unsupported
		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_ENCRYPTION_PARAMETERS), "OID4VCI-1FINAL-8.3.1");
	}
}
