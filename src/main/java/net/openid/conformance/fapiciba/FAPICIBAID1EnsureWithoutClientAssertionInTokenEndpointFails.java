package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAuthReqIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddClientIdToRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForCIBAGrant;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-without-client-assertion-in-token-endpoint-request",
	displayName = "FAPI-CIBA-ID1: Ensure without client_assertion in token endpoint request",
	summary = "This test passes client_id into token endpoint request instead of client_assertion. The token endpoint return an error message that is 'invalid_request' or 'invalid_client'.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
public class FAPICIBAID1EnsureWithoutClientAssertionInTokenEndpointFails extends AbstractFAPICIBAID1 {

	@Override
	protected void performPostAuthorizationResponse() {
		callAndStopOnFailure(CreateTokenEndpointRequestForCIBAGrant.class);
		callAndStopOnFailure(AddAuthReqIdToTokenEndpointRequest.class);

		mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
		callAndStopOnFailure(AddClientIdToRequest.class);
		unmapClientAuthKeys();

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");

		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		validateErrorFromTokenEndpointResponse();
		callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
