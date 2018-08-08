package io.fintechlabs.testframework.openbanking;

import java.util.Map;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

/**
 * @author ddrysdale
 *
 */

public abstract class AbstractOBUserRejectsAuthenticationCodeIdToken extends AbstractOBServerTestModuleCodeIdToken {

	public AbstractOBUserRejectsAuthenticationCodeIdToken(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	@Override
	protected void createAuthorizationRequest() {

		env.putInteger("requested_state_length", 128);

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
	}

	@Override
	protected Object onAuthorizationCallbackResponse() {

		callAndStopOnFailure(ValidateUserRejectsAuthorizationParametersCorrect.class, "OIDCC-3.1.2.6");

		fireTestFinished();

		return redirectToLogDetailPage();
	}

}
