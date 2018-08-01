package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.client.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

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

		browser.goToUrl(redirectTo);

		setStatus(Status.WAITING);
	}

}
