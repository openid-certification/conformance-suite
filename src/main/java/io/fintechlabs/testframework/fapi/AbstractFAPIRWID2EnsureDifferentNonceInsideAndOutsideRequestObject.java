package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIncorrectNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestError;
import io.fintechlabs.testframework.condition.client.ExpectRequestDifferentNonceInsideAndOutsideErrorPage;
import io.fintechlabs.testframework.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;

public abstract class AbstractFAPIRWID2EnsureDifferentNonceInsideAndOutsideRequestObject extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performAuthorizationFlow() {

		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestDifferentNonceInsideAndOutsideErrorPage.class, "OIDCC-6.1");

		env.putString("error_callback_placeholder", "request_unverifiable_error");
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(AddIncorrectNonceToAuthorizationEndpointRequest.class, "OIDCC-6.1");

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)
		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			super.onAuthorizationCallbackResponse();

		} else {
			/* If we get an error back from the authorisation server:
			 * - It must be a 'invalid_request' error
			 * - It must have the correct state we supplied
			 */

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
			fireTestFinished();
		}
	}

	@Override
	protected void performPostAuthorizationFlow() {
		// call the token endpoint and complete the flow

		createAuthorizationCodeRequest();

		requestAuthorizationCode();

		checkAccountRequestEndpointTLS();

		checkAccountResourceEndpointTLS();

		requestProtectedResource();

		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");

		callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "RFC7231-5.3.2");

		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);

		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);

		fireTestFinished();
	}

}
