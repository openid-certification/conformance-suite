package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectMissingScopeErrorPage;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public abstract class AbstractFAPIRWID2EnsureRequestObjectWithoutScopeFails extends AbstractFAPIRWID2ServerTestModule {

	protected AbstractFAPIRWID2EnsureRequestObjectWithoutScopeFails(StepsConfiguration stepsConfiguration) {
		super(stepsConfiguration);
	}

	@Override
	protected void performAuthorizationFlow() {
		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingScopeErrorPage.class, "FAPI-RW-5.2.3-8");

		env.putString("error_callback_placeholder", "request_object_unverifiable_error");
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpToRequestObject.class);

		String scope = OIDFJSON.getString(env.getObject("request_object_claims").get("scope"));
		env.getObject("request_object_claims").remove("scope");

		callAndStopOnFailure(SignRequestObject.class);

		env.getObject("request_object_claims").addProperty("scope", scope);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorisation server:
		 * - It must be a 'invalid_request_object', 'invalid_request' or 'access_denied' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");
		fireTestFinished();
	}

}
