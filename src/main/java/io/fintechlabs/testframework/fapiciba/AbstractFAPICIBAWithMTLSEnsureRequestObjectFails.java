package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CreateLongRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromBackchannelAuthenticationEndpointResponse;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse;

public abstract class AbstractFAPICIBAWithMTLSEnsureRequestObjectFails extends AbstractFAPICIBAWithMTLS {

	protected void onCallBackChannelAuthenticationEndpointResponse() {

		callAndStopOnFailure(EnsureErrorFromBackchannelAuthenticationEndpointResponse.class);

		callAndStopOnFailure(EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse.class);

		fireTestFinished();

	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {

		if ( whichClient == 2 ) {
			callAndStopOnFailure(CreateLongRandomClientNotificationToken.class, "CIBA-7.1", "RFC6750-2.1");
		} else {
			callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");
		}

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
