package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CheckBackchannelAuthenticationEndpointErrorHttpStatus;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointError;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ensure-without-client-assertion-in-backchannel-authorization-request",
	displayName = "FAPI-CIBA: Ensure without client_assertion in backchannel authorization request",
	summary = "This test passes client_id into request instead of client_assertion to the backchannel authorization endpoint, and should end with the server returning an access_denied or invalid_request or invalid_client error",
	profile = "FAPI-CIBA",
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
	}
)
public class FAPICIBAEnsureWithoutClientAssertionInBackchannelAuthorizationRequestFails extends AbstractFAPICIBA {

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
	protected void addClientAuthenticationToBackchannelRequest() {

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		eventLog.endBlock();

		validateErrorFromBackchannelAuthorizationRequestResponse();

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void validateErrorFromBackchannelAuthorizationRequestResponse() {
		callAndContinueOnFailure(ValidateErrorResponseFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(ValidateErrorUriFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointError.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(CheckBackchannelAuthenticationEndpointErrorHttpStatus.class, Condition.ConditionResult.FAILURE, "CIBA-13");
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		//Not called in this test
	}

	protected void performPostAuthorizationFlow() {
		// we shouldn't get here anyway, but if we do, just check access token, don't go on and try second client
		requestProtectedResource();
		fireTestFinished();
	}
}
