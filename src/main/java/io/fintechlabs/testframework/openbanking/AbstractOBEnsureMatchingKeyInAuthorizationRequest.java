package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.ExpectInvalidRequestObjectError;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractOBEnsureMatchingKeyInAuthorizationRequest extends AbstractOBServerTestModule {

	public AbstractOBEnsureMatchingKeyInAuthorizationRequest(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		// Switch to client 2 JWKs

		eventLog.startBlock("Second client's keys");
		env.mapKey("client_jwks", "client_jwks2");

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		callAndStopOnFailure(ExpectRequestObjectUnverifiableErrorPage.class, "FAPI-2-5.2.2-1");

		eventLog.endBlock();
		env.unmapKey("client_jwks");

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
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

		env.put("callback_params", requestParts.get("params").getAsJsonObject());
		env.put("callback_query_params", requestParts.get("params").getAsJsonObject());

		call(ValidateErrorResponseFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		call(ExpectInvalidRequestObjectError.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();

		// as we got an answer from the browser, we could mark the image placeholder as satisfied, but that's hard

		return redirectToLogDetailPage();
	}

}
