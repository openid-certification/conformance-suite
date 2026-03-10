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
	profile = "FAPI-CIBA-ID1"
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
