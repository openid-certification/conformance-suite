package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;

/**
 * @author ddrysdale
 *
 */

public abstract class AbstractOBUserRejectsAuthenticationCode extends AbstractOBServerTestModuleCode {

	@Override
	protected void createAuthorizationRequest() {

		env.putInteger("requested_state_length", 128);

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
	}

	@Override
	protected Object onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectAccessDeniedErrorFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		fireTestFinished();

		return redirectToLogDetailPage();
	}

}
