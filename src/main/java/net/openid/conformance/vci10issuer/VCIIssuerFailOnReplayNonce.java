package net.openid.conformance.vci10issuer;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialResponseEncryptionToRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

/**
 * Negative test that verifies the credential issuer properly rejects nonce replay attempts.
 *
 * This test first completes a successful credential issuance flow, then attempts to make
 * a second credential request using the same proof/nonce. The credential issuer must reject
 * the replayed nonce with an invalid_nonce error.
 *
 * Per OID4VCI Section 8.3.1, the Credential Issuer should return invalid_nonce when the
 * nonce has already been used or is otherwise invalid.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3.1">OID4VCI Section 8.3.1 - Credential Error Response</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-replay-nonce",
	displayName = "OID4VCI 1.0: Issuer fail on replayed nonce",
	summary = "This test case verifies proper nonce replay protection. It first completes a successful " +
		"credential issuance flow, then attempts a second credential request using the same nonce/proof. " +
		"The credential issuer must reject the replayed nonce with an invalid_nonce error (HTTP 400). " +
		"Note: This test requires a credential configuration that requires cryptographic binding (proof). " +
		"If the selected credential configuration does not require proof, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnReplayNonce extends VCIIssuerHappyFlow {

	@Override
	public void start() {
		// Skip this test if the credential configuration doesn't require cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("This test requires a credential configuration with cryptographic binding (proof). " +
				"The selected credential configuration does not require proof, so nonce replay protection cannot be tested.");
			return;
		}

		super.start();
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		// First, verify that the initial credential request succeeded
		super.verifyCredentialIssuerCredentialResponse();

		// Now attempt to replay the same nonce by making another credential request
		// with the same proof JWT (which contains the same nonce)
		eventLog.startBlock("Attempt nonce replay");

		// The proof in credential_request_proofs was already used in the first request
		// We create a new credential request using the same proof object
		callAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		// Add encryption parameters if encryption is enabled (same as the first request)
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIAddCredentialResponseEncryptionToRequest.class, "OID4VCI-1FINAL-11.2.3");
		}

		JsonObject credentialRequestObject = env.getObject("vci_credential_request_object");
		String requestBodyString = credentialRequestObject.toString();
		env.putString("resource_request_entity", requestBodyString);

		// Make the second credential request with the replayed nonce
		// For DPoP, we may need to retry if the server requires a new DPoP nonce after the first successful request
		if (isDpop() && createDpopForResourceEndpointSteps != null) {
			final int MAX_RETRY = 2;
			int i = 0;
			while (i < MAX_RETRY) {
				call(sequence(createDpopForResourceEndpointSteps));
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, "OID4VCI-1FINAL-8");
				if (Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-8");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		// Verify the replayed nonce is rejected with invalid_nonce error
		// Use callAndStopOnFailure for HTTP status check - if the server returns 200,
		// it means it accepted the replayed nonce (no replay protection), and we should
		// stop with a clear failure rather than trying to parse a success response as an error
		callAndStopOnFailure(EnsureHttpStatusCodeIs400.class, "OID4VCI-1FINAL-8.3.1");

		callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialErrorResponse.class, Condition.ConditionResult.WARNING, "OID4VCI-1FINAL-8.3.1");
		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_NONCE), "OID4VCI-1FINAL-8.3.1");

		call(exec().unmapKey("endpoint_response"));

		eventLog.endBlock();
	}
}
