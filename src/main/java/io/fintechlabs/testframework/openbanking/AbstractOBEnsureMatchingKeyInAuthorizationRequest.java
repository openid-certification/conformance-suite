package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.client.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClient2Configuration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInJWKs;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureMatchingKeyInAuthorizationRequest extends AbstractOBServerTestModule {

	public AbstractOBEnsureMatchingKeyInAuthorizationRequest(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		// Switch to client 2 JWKs

		callAndStopOnFailure(GetStaticClient2Configuration.class);

		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInJWKs.class, "OIDCC-10.1");

		callAndStopOnFailure(GetStaticClientConfiguration.class);

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		callAndStopOnFailure(ExpectRequestObjectUnverifiableErrorPage.class, "FAPI-2-5.2.2-1");

		browser.goToUrl(redirectTo);

		setStatus(Status.WAITING);
	}

}
