package net.openid.conformance.vci10issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AddRandomFieldsToJsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddRandomParameterToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainNonRequestedClaims;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.vci10issuer.condition.VCIAddCompressionToCredentialRequestEncryption;
import net.openid.conformance.vci10issuer.condition.VCICheckCredentialResponseCompression;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-happy-flow",
	displayName = "OID4VCI 1.0: Issuer happy flow",
	summary = "This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using Pushed Authorization Requests (PAR), and an access token is obtained. The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce, and successfully requests a credential from the Credential Endpoint. The wallet also sprinkles random unknown fields into the JSON bodies of the authorization, token, credential and notification requests to verify that the issuer ignores unknown fields as required by the specification.",
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

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		return super.makeCreateAuthorizationRequestSteps(usePkce)
			.then(condition(AddRandomParameterToAuthorizationEndpointRequest.class)
				.requirements("OID4VCI-1FINAL-5.1.3"));
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		super.createAuthorizationCodeRequest();
		addRandomFieldsToTokenEndpointRequest();
	}

	@Override
	protected void createPreAuthorizationCodeRequest() {
		super.createPreAuthorizationCodeRequest();
		addRandomFieldsToTokenEndpointRequest();
	}

	protected void addRandomFieldsToTokenEndpointRequest() {
		callAndStopOnFailure(
			new AddRandomFieldsToJsonObject("token endpoint request body", "token_endpoint_request_form_parameters"),
			"OID4VCI-1FINAL-6.1");
	}

	@Override
	protected String serializeCredentialRequestObject(JsonObject credentialRequestObject) {
		// Inject a random top-level unknown field so we can verify the issuer ignores it per
		// OID4VCI 1.0 §8.2 ("The Credential Issuer MUST ignore any unrecognized parameters").
		// We deliberately do not inject into nested objects: 'proofs' is required by §8.2 to
		// contain exactly one parameter (named as the proof type), and the §8.2 ignore-unknowns
		// clause does not explicitly extend to tightly-defined nested structures such as
		// 'credential_response_encryption'.
		callAndStopOnFailure(
			new AddRandomFieldsToJsonObject("credential request", "vci_credential_request_object"),
			"OID4VCI-1FINAL-8.2");
		return super.serializeCredentialRequestObject(credentialRequestObject);
	}

	@Override
	protected void createNotificationRequest() {
		super.createNotificationRequest();
		callAndStopOnFailure(
			new AddRandomFieldsToJsonObject("notification request body", "notification_request_body"),
			"OID4VCI-1FINAL-11.1");
		env.putString("resource_request_entity", env.getObject("notification_request_body").toString());
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
