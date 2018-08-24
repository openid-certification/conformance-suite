package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.client.ExpectRedirectUriMissingErrorPage;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

public abstract class AbstractOBEnsureRedirectUriInAuthorizationRequest extends AbstractOBServerTestModule {

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

		callAndStopOnFailure(ExpectRedirectUriMissingErrorPage.class, "FAPI-1-5.2.2-9");

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

}
