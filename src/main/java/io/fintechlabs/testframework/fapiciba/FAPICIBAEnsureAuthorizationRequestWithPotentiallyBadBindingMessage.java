package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddPotentiallyBadBindingMessageToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage;
import io.fintechlabs.testframework.condition.client.ExpectBindingMessageCorrectDisplay;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ensure-authorization-request-with-potentially-bad-binding-message",
	displayName = "FAPI-CIBA: Test with a potentially bad binding message, the server should authenticate successfully or return the invalid_binding_message error",
	summary = "This test tries sending a potentially bad binding message to authorization endpoint request. The server should either authenticate successfully showing the correct binding message (a screenshot/photo of which should be uploaded) or return the invalid_binding_message error.",
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
public class FAPICIBAEnsureAuthorizationRequestWithPotentiallyBadBindingMessage extends AbstractFAPICIBA {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Variant(name = variant_poll_privatekeyjwt)
	public void setupPollPrivateKeyJwt() {
		super.setupPollPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		super.setupOpenBankingUkPingMTLS();
	}

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		super.setupOpenBankingUkPollMTLS();
	}

	@Variant(name = variant_openbankinguk_poll_privatekeyjwt)
	public void setupOpenBankingUkPollPrivateKeyJwt() {
		super.setupOpenBankingUkPollPrivateKeyJwt();
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(AddPotentiallyBadBindingMessageToAuthorizationEndpointRequest.class, "CIBA-7.1");

	}

	@Override
	protected void performAuthorizationFlow() {

		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		JsonObject callbackParams = env.getObject("backchannel_authentication_endpoint_response");

		if (callbackParams != null && callbackParams.has("error")) {

			validateErrorFromBackchannelAuthorizationRequestResponse();

			callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage.class, Condition.ConditionResult.FAILURE, "CIBA-13");

			cleanUpPingTestResources();
			fireTestFinished();

		} else {

			performValidateAuthorizationResponse();

			eventLog.endBlock();

			performPostAuthorizationResponse();

		}
	}

	@Override
	protected void performPostAuthorizationFlow(boolean finishTest) {

		requestProtectedResource();

		setStatus(Status.WAITING);

		// ask the user to upload a screenshot/photo of the binding message being correctly displayed when the server authenticates successfully
		callAndContinueOnFailure(ExpectBindingMessageCorrectDisplay.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");

		waitForPlaceholders();

		cleanUpPingTestResources();
	}
}
