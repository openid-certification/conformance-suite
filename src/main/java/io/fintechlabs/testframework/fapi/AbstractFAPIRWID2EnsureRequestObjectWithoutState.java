package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectMissingStateErrorPage;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.VerifyNoSHash;
import io.fintechlabs.testframework.condition.client.VerifyNoStateInAuthorizationResponse;

public abstract class AbstractFAPIRWID2EnsureRequestObjectWithoutState extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingStateErrorPage.class, "FAPI-RW-5.2.3-8");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		performProfileAuthorizationEndpointSetup();

		env.putInteger("requested_state_length", null);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)
		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			callAndStopOnFailure(CheckMatchingCallbackParameters.class);

			callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

			callAndContinueOnFailure(VerifyNoStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

			handleSuccessfulAuthorizationEndpointResponse();

		} else {
			/* If we get an error back from the authorisation server:
			 * - It must be a 'invalid_request_object', 'invalid_request' or 'access_denied' error
			 * - It must have the correct state we supplied
			 */

			// state can be absented if authorization request did not send state in the request object
			skipIfElementMissing("authorization_endpoint_response",  "state", Condition.ConditionResult.INFO,
				CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");

			fireTestFinished();
		}
	}

	@Override
	protected void handleSuccessfulAuthorizationEndpointResponse() {
		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");

		// save the id_token returned from the authorisation endpoint
		env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

		performIdTokenValidation();

		// s_hash must not be returned, as AS must ignore the state parameter outside the request object
		callAndContinueOnFailure(VerifyNoSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-10");

		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		performPostAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationFlow() {
		// call the token endpoint and complete the flow

		createAuthorizationCodeRequest();

		requestAuthorizationCode();

		checkAccountRequestEndpointTLS();

		checkAccountResourceEndpointTLS();

		requestProtectedResource();

		verifyAccessTokenWithResourceEndpoint();

		fireTestFinished();
	}

}
