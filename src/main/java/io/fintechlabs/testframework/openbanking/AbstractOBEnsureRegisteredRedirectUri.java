package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.client.CreateBadRedirectUri;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.common.ExpectRedirectUriErrorPage;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureRegisteredRedirectUri extends AbstractOBServerTestModule {

	public AbstractOBEnsureRegisteredRedirectUri(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		// create a random redirect URI
		callAndStopOnFailure(CreateBadRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "FAPI-1-5.2.2-8");

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
