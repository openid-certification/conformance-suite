package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest;

// Send invalid request to backchannel authorisation endpoint and the response is invalid_request
public abstract class AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorisationRequest extends AbstractFAPICIBAID1 {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		eventLog.endBlock();

		validateErrorFromBackchannelAuthorizationRequestResponse();

		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		//Not called in this test
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
