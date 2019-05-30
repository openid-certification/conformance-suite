package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;

public abstract class AbstractFAPICIBAWithMTLSEnsureRequestObjectFails extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequestWithMTLS {

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {

		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		/* Nothing to do */
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		/* Nothing to do */
	}

}
