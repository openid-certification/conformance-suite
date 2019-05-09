package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.AddMultipleHintsToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CreateEmptyAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromBackchannelAuthenticationEndpointResponse;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse;

public abstract class AbstractFAPICIBAEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");
		callAndStopOnFailure(AddMultipleHintsToAuthorizationEndpointRequestResponse.class, "CIBA-7.2-3");

		// The spec also defines these parameters that we don't currently set:
		// acr_values
		// binding_message
		// user_code
		// requested_expiry

		modeSpecificAuthorizationEndpointRequest();

		performProfileAuthorizationEndpointSetup();

	}

	@Override
	protected void onCallBackChannelAuthenticationEndpointResponse() {

		callAndStopOnFailure(EnsureErrorFromBackchannelAuthenticationEndpointResponse.class);

		callAndStopOnFailure(EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse.class);

		fireTestFinished();

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
