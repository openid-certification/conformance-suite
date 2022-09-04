package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddJtiToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.CallBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.InvalidateRequestObjectSignature;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-signature-algorithm-is-bad-fails",
	displayName = "FAPI-CIBA-ID1: Ensure request_object signature algorithm is bad fails",
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
public class FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsBadFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void performAuthorizationRequest() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);
		callAndStopOnFailure(AddIatToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddExpToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddNbfToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddJtiToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddAudToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddIssToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(SignRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(InvalidateRequestObjectSignature.class, "CIBA-7.2");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		addClientAuthenticationToBackchannelRequest();

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);
	}
}
