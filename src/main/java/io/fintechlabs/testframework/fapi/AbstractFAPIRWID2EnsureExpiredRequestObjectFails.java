package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddExpiredExpToRequestObject;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestObjectError;
import io.fintechlabs.testframework.condition.client.ExpectExpiredRequestObjectClaimErrorPage;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;

public abstract class AbstractFAPIRWID2EnsureExpiredRequestObjectFails extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performAuthorizationFlow() {
		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		callAndStopOnFailure(ExpectExpiredRequestObjectClaimErrorPage.class, "RFC7519-4.1.4");

		setStatus(Status.WAITING);

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpiredExpToRequestObject.class, "RFC7519-4.1.4");

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorisation server:
		 * - It must be a 'invalid_request_object' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();

	}

}
