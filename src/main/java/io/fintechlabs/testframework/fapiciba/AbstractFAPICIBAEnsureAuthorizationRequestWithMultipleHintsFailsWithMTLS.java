package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddMultipleHintsToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateEmptyAuthorizationEndpointRequest;

public abstract class AbstractFAPICIBAEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequestWithMTLS {

	@Override
	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequest.class, "CIBA-7.1");
		callAndStopOnFailure(AddMultipleHintsToAuthorizationEndpointRequest.class, "CIBA-7.2-3");

		// The spec also defines these parameters that we don't currently set:
		// acr_values
		// binding_message
		// user_code
		// requested_expiry

		modeSpecificAuthorizationEndpointRequest();

		performProfileAuthorizationEndpointSetup();

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
