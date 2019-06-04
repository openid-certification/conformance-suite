package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddHintToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateEmptyAuthorizationEndpointRequest;

public abstract class AbstractFAPICIBAEnsureAuthorizationRequestWithBindingMessageSucceedsWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();

		callAndStopOnFailure(AddBindingMessageToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}

}
