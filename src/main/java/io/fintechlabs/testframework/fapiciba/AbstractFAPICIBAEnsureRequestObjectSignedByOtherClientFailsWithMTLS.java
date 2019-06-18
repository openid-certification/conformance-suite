package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignAuthenticationRequest;

public abstract class AbstractFAPICIBAEnsureRequestObjectSignedByOtherClientFailsWithMTLS extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequest {

	@Override
	protected void performAuthorizationRequest() {
		createAuthorizationRequestObject();

		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Jwks2");
		env.mapKey("client_jwks", "client_jwks2");

		// aud, iss are added by SignRequestObject
		callAndStopOnFailure(SignAuthenticationRequest.class, "CIBA-7.1.1");

		env.unmapKey("client_jwks");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);
		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);

		eventLog.endBlock();
	}

}
