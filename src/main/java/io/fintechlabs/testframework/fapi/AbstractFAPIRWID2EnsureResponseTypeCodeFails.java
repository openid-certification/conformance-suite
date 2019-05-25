package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.DetectWhetherErrorResponseIsInQueryOrFragment;
import io.fintechlabs.testframework.condition.client.EnsureUnsupportedResponseTypeErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlFragment;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.common.ExpectGrantTypeErrorPage;

public abstract class AbstractFAPIRWID2EnsureResponseTypeCodeFails extends AbstractFAPIRWID2ServerTestModule {

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
	protected void processCallback() {

		eventLog.startBlock(currentClientString() + "Verify authorization endpoint error response");

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");
		callAndContinueOnFailure(RejectAuthCodeInUrlFragment.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		// It doesn't really matter if the error in the fragment or the query, the specs aren't entirely clear on the matter
		callAndStopOnFailure(DetectWhetherErrorResponseIsInQueryOrFragment.class);

		/* The error from the authorisation server:
		 * - must be a 'unsupported_response_type' error
		 * - must have the correct state we supplied
		 */
		callAndContinueOnFailure(EnsureUnsupportedResponseTypeErrorFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		eventLog.endBlock();
		fireTestFinished();
	}

}
