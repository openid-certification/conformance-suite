package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddRedirectUriQuerySuffix;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.ExpectInvalidRequestObjectError;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.condition.client.RedirectQueryTestDisabled;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractOBEnsureServerHandlesNonMatchingIntentId extends AbstractOBServerTestModule {

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Client2, Jwks2, tls2");
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		createAuthorizationRequest();
		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		callAndStopOnFailure(ExpectRequestObjectUnverifiableErrorPage.class, "FAPI-2-5.2.2-1");

		env.unmapKey("mutual_tls_authentication");
		env.unmapKey("client_jwks");
		env.unmapKey("client");

		eventLog.endBlock();

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

		env.putObject("callback_params", requestParts.get("params").getAsJsonObject());
		env.putObject("callback_query_params", requestParts.get("params").getAsJsonObject());

		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectInvalidRequestObjectError.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();

		// as we got an answer from the browser, we could mark the image placeholder as satisfied, but that's hard

		return redirectToLogDetailPage();
	}

}
