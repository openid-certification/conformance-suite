package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddJtiToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNbfToRequestObject;
import io.fintechlabs.testframework.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignRequestObject;

public abstract class AbstractFAPICIBAEnsureRequestObjectSignedByOtherClientFailsWithMTLS extends AbstractFAPICIBAEnsureSendingInvalidSignedRequestObjectWithMTLS {

	@Override
	protected void performAuthorizationRequest() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);
		callAndStopOnFailure(AddIatToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddExpToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddNbfToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddJtiToRequestObject.class, "CIBA-7.1.1");

		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Jwks2");
		env.mapKey("client_jwks", "client_jwks2");

		// aud, iss are added by SignRequestObject
		callAndStopOnFailure(SignRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);
		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);

		env.unmapKey("client_jwks");

		eventLog.endBlock();
	}

}
