package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddRequestObjectClaimsToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.CallBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-unsigned-backchannel-authorization-request-fails",
	displayName = "FAPI-CIBA-ID1: Ensure unsigned backchannel authorization request fails",
	summary = "This test should end with the backchannel authorization server returning an error message that the request is invalid, as FAPI-CIBA requires the use of signed authentication requests.",
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
public class FAPICIBAID1EnsureBackchannelAuthorizationRequestWithoutRequestFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void performAuthorizationRequest() {

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		addClientAuthenticationToBackchannelRequest();

		callAndStopOnFailure(AddRequestObjectClaimsToBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);
	}

}
