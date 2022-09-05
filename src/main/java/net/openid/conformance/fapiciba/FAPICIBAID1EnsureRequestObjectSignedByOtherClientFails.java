package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.CallBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-signed-by-other-client-fails",
	displayName = "FAPI-CIBA-ID1: Ensure request_object signed by other client fails",
	summary = "This test should end with the backchannel authorization server returning an error message that the request is invalid.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAID1EnsureRequestObjectSignedByOtherClientFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void configClient() {
		setupClient1();

		setupClient2();
	}

	@Override
	protected void performAuthorizationRequest() {
		createAuthorizationRequestObject();

		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Jwks2");
		env.mapKey("client_jwks", "client_jwks2");

		callAndStopOnFailure(SignRequestObject.class, "CIBA-7.1.1");

		env.unmapKey("client_jwks");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		addClientAuthenticationToBackchannelRequest();

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);

		eventLog.endBlock();
	}

	@Override
	public void cleanup() {
		unregisterClient1();

		unregisterClient2();
	}
}
