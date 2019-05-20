package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus401;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;

public abstract class AbstractFAPICIBAEnsureWrongClientIdInTokenEndpointRequestWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void performPostAuthorizationResponse() {

		eventLog.startBlock(currentClientString() + "Swapping to Client2 and calling token endpoint");
		env.mapKey("client", "client2");

		callTokenEndpointForCibaGrant();

		env.unmapKey("client");
		eventLog.endBlock();

		callAndStopOnFailure(CheckTokenEndpointHttpStatus401.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");

		fireTestFinished();
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		//Not called in this test

	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		//Not called in this test
	}

}
