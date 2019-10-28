package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAlgorithmAsRS256;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusForInvalidRequestOrInvalidClientError;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-client-assertion-signature-algorithm-in-token-endpoint-request-is-RS256-fails",
	displayName = "FAPI-CIBA-ID1: Ensure client_assertion signature algorithm in token endpoint request is RS256 fails",
	summary = "This test should end with the token endpoint returning an error message that the client is invalid.",
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
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
public class FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInTokenEndpointRequestIsRS256Fails extends AbstractFAPICIBAID1 {

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		callAndStopOnFailure(AddAlgorithmAsRS256.class, "FAPI-RW-8.6");

		super.addClientAuthenticationToTokenEndpointRequest();
	}

	protected void performPostAuthorizationResponse() {
		callTokenEndpointForCibaGrant();

		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		validateErrorFromTokenEndpointResponse();
		callAndContinueOnFailure(CheckTokenEndpointHttpStatusForInvalidRequestOrInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
