package io.fintechlabs.testframework.openbanking;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.ExpectInvalidRequestObjectError;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.condition.client.SerializeRequestObjectWithNullAlgorithm;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

public abstract class AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNone extends AbstractOBServerTestModule {

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		callAndStopOnFailure(ExpectRequestObjectUnverifiableErrorPage.class, "FAPI-2-7.3-1");

		setStatus(Status.WAITING);

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRedirect() {

		env.putBoolean("expose_state_in_authorization_endpoint_request", true);

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		// dispatch based on the path
		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	@UserFacing
	private Object handleCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		/* If we get an error back from the authorisation server:
		 * - It must be in the query (even in hybrid flow): https://openid.net/specs/openid-connect-core-1_0.html#HybridAuthError
		 * - It must be a 'invalid_request_object' error
		 * - It must have the correct state we supplied
		 */

		env.putObject("callback_params", requestParts.get("params").getAsJsonObject());
		env.putObject("callback_query_params", requestParts.get("params").getAsJsonObject());

		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		// as we got an answer from the browser, we could mark the image placeholder as satisfied, but that's hard

		fireTestFinished();

		return redirectToLogDetailPage();
	}

}
