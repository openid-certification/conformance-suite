package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.EnsureUnsupportedGrantTypeErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.common.ExpectGrantTypeErrorPage;

public abstract class AbstractFAPIRWEnsureResponseTypeCodeFails extends AbstractFAPIRWServerTestModule {

	@Override
	protected void performAuthorizationFlow() {

		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		callAndStopOnFailure(ExpectGrantTypeErrorPage.class, "FAPI-RW-5.2.2-2");

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString("grant_type_error"));
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class, "FAPI-RW-5.2.2-2");

		super.createAuthorizationRedirect();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorisation server:
		 * - It must be a 'unsupported_response_type' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureUnsupportedGrantTypeErrorFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
		fireTestFinished();
	}

}
