package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;


public abstract class AbstractFAPIRWID2UserRejectsAuthentication extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void createAuthorizationRequest() {

		env.putInteger("requested_state_length", 128);

		super.createAuthorizationRequest();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectAccessDeniedErrorFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		if (whichClient == 1) {
			performAuthorizationFlowWithSecondClient();
		} else {

			// Check if server return correct params as we requested in redirect_uri query part
			callAndStopOnFailure(CheckMatchingCallbackParameters.class);
			fireTestFinished();
		}
	}

}
