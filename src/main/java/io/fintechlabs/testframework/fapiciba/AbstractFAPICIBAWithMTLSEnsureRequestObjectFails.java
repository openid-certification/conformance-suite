package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CreateLongRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromBackchannelAuthenticationEndpointResponse;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse;

public abstract class AbstractFAPICIBAWithMTLSEnsureRequestObjectFails extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequestWithMTLS {

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {

		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");
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
