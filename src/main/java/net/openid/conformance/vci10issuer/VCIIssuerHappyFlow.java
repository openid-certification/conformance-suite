package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainNonRequestedClaims;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.vci10issuer.condition.VCIAddCompressionToCredentialRequestEncryption;
import net.openid.conformance.vci10issuer.condition.VCICheckCredentialResponseCompression;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-happy-flow",
	displayName = "OID4VCI 1.0: Issuer happy flow",
	summary = "This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using Pushed Authorization Requests (PAR), and an access token is obtained. The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce, and successfully requests a credential from the Credential Endpoint.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerHappyFlow extends AbstractVCIIssuerTestModule {

	private boolean addCompressionToNextRequest;

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if (isOpenId && !isSecondClient()) {
			callAndContinueOnFailure(EnsureIdTokenDoesNotContainNonRequestedClaims.class, Condition.ConditionResult.WARNING);
		}

		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			performCompressedEncryptionRequest();
		}

		super.onPostAuthorizationFlowComplete();
	}

	@Override
	protected void afterCredentialResponseEncryptionAdded() {
		super.afterCredentialResponseEncryptionAdded();
		if (addCompressionToNextRequest) {
			callAndStopOnFailure(VCIAddCompressionToCredentialRequestEncryption.class, "OID4VCI-1FINAL-8.2");
		}
	}

	/**
	 * Makes a second credential request with encryption + DEFLATE compression (zip=DEF)
	 * to test that the issuer handles compression correctly.
	 */
	protected void performCompressedEncryptionRequest() {
		eventLog.startBlock("Credential request with encryption and DEFLATE compression");

		addCompressionToNextRequest = true;
		refreshCredentialRequest();
		addCompressionToNextRequest = false;

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-8");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		verifyCredentialIssuerCredentialResponse();

		callAndContinueOnFailure(VCICheckCredentialResponseCompression.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.2");

		call(exec().unmapKey("endpoint_response"));

		eventLog.endBlock();
	}
}
