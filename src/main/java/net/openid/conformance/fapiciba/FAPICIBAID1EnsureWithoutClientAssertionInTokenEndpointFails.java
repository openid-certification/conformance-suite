package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAuthReqIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusForInvalidRequestOrInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForCIBAGrant;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.Variant;

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
		callAndContinueOnFailure(CheckTokenEndpointHttpStatusForInvalidRequestOrInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
