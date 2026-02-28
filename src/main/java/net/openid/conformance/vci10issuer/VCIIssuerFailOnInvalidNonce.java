package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIInjectRandomCNonce;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-nonce",
	displayName = "OID4VCI 1.0: Issuer fail on invalid nonce",
	summary = "This test case checks for the proper error handling during the standard credential issuance flow using an emulated wallet when an invalid c_nonce is used. " +
		"Note: This test requires a credential configuration that requires cryptographic binding (proof). " +
		"If the selected credential configuration does not require proof, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidNonce extends VCIIssuerHappyFlow {

	@Override
	public void start() {
		// Skip this test if the credential configuration doesn't require cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("This test requires a credential configuration with cryptographic binding (proof). " +
				"The selected credential configuration does not require proof, so nonce validation cannot be tested.");
			return;
		}

		super.start();
	}

	@Override
	protected void afterNonceEndpointResponse() {
		super.afterNonceEndpointResponse();

		callAndContinueOnFailure(VCIInjectRandomCNonce.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		verifyCredentialIssuerCredentialErrorResponse();

		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_NONCE), "OID4VCI-1FINAL-8.3.1");
	}
}
