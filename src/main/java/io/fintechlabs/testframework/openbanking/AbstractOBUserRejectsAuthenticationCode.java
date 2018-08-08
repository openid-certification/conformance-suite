package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

/**
 * @author ddrysdale
 *
 */

public abstract class AbstractOBUserRejectsAuthenticationCode extends AbstractOBServerTestModuleCode {

	public AbstractOBUserRejectsAuthenticationCode(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	@Override
	protected void createAuthorizationRequest() {

		env.putInteger("requested_state_length", 128);

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
	}

	@Override
	protected Object onAuthorizationCallbackResponse() {

		call(ValidateUserRejectsAuthorizationParametersCorrect.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		call(ExpectAccessDeniedErrorFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		fireTestFinished();

		return redirectToLogDetailPage();
	}

}
