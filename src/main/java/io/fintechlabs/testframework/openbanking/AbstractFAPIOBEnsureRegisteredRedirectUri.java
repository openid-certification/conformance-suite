package io.fintechlabs.testframework.openbanking;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.client.CreateBadRedirectUri;
import io.fintechlabs.testframework.condition.common.ExpectRedirectUriErrorPage;

public abstract class AbstractFAPIOBEnsureRegisteredRedirectUri extends AbstractFAPIOBServerTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		// create a random redirect URI
		callAndStopOnFailure(CreateBadRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "FAPI-R-5.2.2-8");

		setStatus(Status.WAITING);

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString("redirect_uri_error"));
	}

}
