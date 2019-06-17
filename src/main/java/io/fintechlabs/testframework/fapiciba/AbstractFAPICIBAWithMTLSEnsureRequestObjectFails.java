package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CIBANotificationEndpointCalledUnexpectedly;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;

public abstract class AbstractFAPICIBAWithMTLSEnsureRequestObjectFails extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequestWithMTLS {

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {

		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}

	/** we shouldn't get a Ping notification for this test, so don't do anything further if we do */
	protected void processNotificationCallback(JsonObject requestParts) {
		callAndContinueOnFailure(CIBANotificationEndpointCalledUnexpectedly.class, Condition.ConditionResult.FAILURE);
		fireTestFinished();
	}
}
