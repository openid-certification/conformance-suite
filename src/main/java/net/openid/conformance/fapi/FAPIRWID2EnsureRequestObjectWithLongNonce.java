package net.openid.conformance.fapi;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.ExpectRequestObjectWithLongNonceErrorPage;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import net.openid.conformance.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-request-object-with-long-nonce",
	displayName = "FAPI-RW-ID2: ensure request object with long nonce",
	summary = "This test passes a long nonce in request object. The authorization server must either return an invalid_request error back to the client, and must show an error page (saying the server rejects long nonce - upload a screenshot of the error page) or must successfully authenticate and return the nonce correctly.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class FAPIRWID2EnsureRequestObjectWithLongNonce extends AbstractFAPIRWID2ExpectingAuthorizationFailure {

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectRequestObjectWithLongNonceErrorPage.class, Condition.ConditionResult.WARNING);
		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		performProfileAuthorizationEndpointSetup();

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		// Add long nonce with 384 bytes
		env.putInteger("requested_nonce_length", 384);
		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		if (jarm) {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToJWT.class);
		} else {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
		}
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			super.onAuthorizationCallbackResponse();

		} else {

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.WARNING, "OIDCC-3.3.2.6");

			fireTestFinished();
		}
	}

	@Override
	protected void performPostAuthorizationFlow() {
		// call the token endpoint and complete the flow

		createAuthorizationCodeRequest();

		requestAuthorizationCode();

		requestProtectedResource();

		fireTestFinished();
	}

}
