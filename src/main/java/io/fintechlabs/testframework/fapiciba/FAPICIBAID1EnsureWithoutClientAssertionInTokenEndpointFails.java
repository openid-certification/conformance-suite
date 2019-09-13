package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAuthReqIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus401Or400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForCIBAGrant;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-without-client-assertion-in-token-endpoint-request",
	displayName = "FAPI-CIBA-ID1: Ensure without client_assertion in token endpoint request",
	summary = "This test passes client_id into token endpoint request instead of client_assertion. The token endpoint return an error message that is 'invalid_request' or 'invalid_client'.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	},
	notApplicableForVariants = {
		FAPICIBAID1.variant_ping_mtls,
		FAPICIBAID1.variant_poll_mtls,
		FAPICIBAID1.variant_openbankinguk_ping_mtls,
		FAPICIBAID1.variant_openbankinguk_poll_mtls
	}
)
public class FAPICIBAID1EnsureWithoutClientAssertionInTokenEndpointFails extends AbstractFAPICIBAID1 {

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_poll_privatekeyjwt)
	public void setupPollPrivateKeyJwt() {
		super.setupPollPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_poll_privatekeyjwt)
	public void setupOpenBankingUkPollPrivateKeyJwt() {
		super.setupOpenBankingUkPollPrivateKeyJwt();
	}


	@Override
	protected void performPostAuthorizationResponse() {
		callAndStopOnFailure(CreateTokenEndpointRequestForCIBAGrant.class);
		callAndStopOnFailure(AddAuthReqIdToTokenEndpointRequest.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");

		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		validateErrorFromTokenEndpointResponse();
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus401Or400.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		cleanUpPingTestResources();
		fireTestFinished();
	}
}
