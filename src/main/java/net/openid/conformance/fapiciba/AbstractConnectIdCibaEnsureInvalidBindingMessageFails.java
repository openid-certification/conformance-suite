package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage;

public abstract class AbstractConnectIdCibaEnsureInvalidBindingMessageFails extends AbstractFAPICIBAID1 {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		eventLog.endBlock();

		validateErrorFromBackchannelAuthorizationRequestResponse();

		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage.class,
			Condition.ConditionResult.FAILURE, "CIBA-13", "CID-IDA-5.2-10");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		// Not called in this test.
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		fireTestFinished();
	}
}
