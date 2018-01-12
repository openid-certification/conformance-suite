package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.client.ExpectRedirectUriMissingErrorPage;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureRedirectUriInAuthorizationRequest extends AbstractOBServerTestModule {

	public AbstractOBEnsureRedirectUriInAuthorizationRequest(String name, String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(name, id, owner, eventLog, browser, testInfo);
	}

	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);

		// Remove the redirect URL
		env.get("authorization_endpoint_request").remove("redirect_uri");

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		callAndStopOnFailure(ExpectRedirectUriMissingErrorPage.class, "FAPI-1-5.2.2-9");

		browser.goToUrl(redirectTo);

		/**
		 * We never expect the browser to come back from here, our test is done
		 */

		setStatus(Status.FINISHED);

		// someone needs to review this by hand
		setResult(Result.REVIEW);

		stop();
	}

}
