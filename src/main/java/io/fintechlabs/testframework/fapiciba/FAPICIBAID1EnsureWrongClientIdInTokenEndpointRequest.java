package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus401Or400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-wrong-client-id-in-token-endpoint-request",
	displayName = "FAPI-CIBA-ID1: Ensure wrong client_id in token endpoint request",
	summary = "This test should end with the token endpoint server returning an error message that the client is invalid.",
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
		FAPICIBAID1.variant_ping_privatekeyjwt,
		FAPICIBAID1.variant_poll_privatekeyjwt,
		FAPICIBAID1.variant_openbankinguk_ping_privatekeyjwt,
		FAPICIBAID1.variant_openbankinguk_poll_privatekeyjwt
	}
)
public class FAPICIBAID1EnsureWrongClientIdInTokenEndpointRequest extends AbstractFAPICIBAID1 {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		super.setupOpenBankingUkPingMTLS();
	}

	@Variant(name = variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		super.setupOpenBankingUkPollMTLS();
	}

	@Override
	protected void configClient() {
		setupClient1();

		setupClient2();
	}

	@Override
	protected void performPostAuthorizationResponse() {

		eventLog.startBlock(currentClientString() + "Swapping to Client2 and calling token endpoint");
		env.mapKey("client", "client2");

		callTokenEndpointForCibaGrant();

		env.unmapKey("client");
		eventLog.endBlock();

		callAndStopOnFailure(CheckTokenEndpointHttpStatus401Or400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		cleanUpPingTestResources();
		fireTestFinished();
	}

	@Override
	protected void cleanUpPingTestResources() {
		unregisterClient1();

		unregisterClient2();
	}
}
