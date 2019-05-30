package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddHintToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateEmptyAuthorizationEndpointRequest;

public abstract class AbstractFAPICIBAEnsureAuthorizationRequestWithBindingMessageSucceedsWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequest.class, "CIBA-7.1");
		callAndStopOnFailure(AddHintToAuthorizationEndpointRequest.class, "CIBA-7.1");

		// The spec also defines these parameters that we don't currently set:
		// acr_values
		// user_code
		// requested_expiry
		callAndStopOnFailure(AddBindingMessageToAuthorizationEndpointRequest.class, "CIBA-7.1");

		modeSpecificAuthorizationEndpointRequest();

		performProfileAuthorizationEndpointSetup();
	}

}
