package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddBindingMessageToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.AddHintToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CreateEmptyAuthorizationEndpointRequest;

public abstract class AbstractFAPICIBAEnsureAuthorizationRequestWithBindingMessageSucceedsWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");
		callAndStopOnFailure(AddHintToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");

		// The spec also defines these parameters that we don't currently set:
		// acr_values
		// user_code
		// requested_expiry
		callAndStopOnFailure(AddBindingMessageToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");

		modeSpecificAuthorizationEndpointRequest();

		performProfileAuthorizationEndpointSetup();
	}

}
