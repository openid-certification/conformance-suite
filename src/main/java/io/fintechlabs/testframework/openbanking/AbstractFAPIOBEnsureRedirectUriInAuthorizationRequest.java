package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.client.ExpectRedirectUriMissingErrorPage;

public abstract class AbstractFAPIOBEnsureRedirectUriInAuthorizationRequest extends AbstractFAPIOBServerTestModule {

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		// Remove the redirect URL
		env.getObject("authorization_endpoint_request").remove("redirect_uri");

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		callAndStopOnFailure(ExpectRedirectUriMissingErrorPage.class, "FAPI-R-5.2.2-9");

		setStatus(Status.WAITING);

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString("redirect_uri_missing_error"));
	}

}
