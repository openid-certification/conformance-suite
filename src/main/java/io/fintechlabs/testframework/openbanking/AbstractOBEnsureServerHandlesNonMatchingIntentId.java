package io.fintechlabs.testframework.openbanking;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestObjectError;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToCallbackResponse;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;
import org.springframework.web.servlet.ModelAndView;

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
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestObjectError.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();

		// TODO: we got an answer from the browser, we could mark the image placeholder as satisfied
	}
}
