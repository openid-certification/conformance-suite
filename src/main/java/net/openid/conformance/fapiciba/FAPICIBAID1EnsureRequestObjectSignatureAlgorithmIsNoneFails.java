package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.CallBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-signature-algorithm-is-none-fails",
	displayName = "FAPI-CIBA-ID1: Ensure request_object signature algorithm is none fails",
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
public class FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsNoneFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void performAuthorizationRequest() {
		createAuthorizationRequestObject();

		callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class, "CIBA-7.2");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		addClientAuthenticationToBackchannelRequest();

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);
	}

}
