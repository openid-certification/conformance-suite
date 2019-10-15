package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckBackchannelAuthenticationEndpointErrorHttpStatus;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointError;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-wrong-client-id-in-backchannel-authorization-request",
	displayName = "FAPI-CIBA-ID1: Ensure wrong client_id in backchannel authorization request",
	summary = "This test sends the wrong client_id for the MTLS key to the backchannel authorization endpoint, and should end with the server returning an access_denied or invalid_request or invalid_client error",
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
public class FAPICIBAID1EnsureWrongClientIdInBackchannelAuthorizationRequest extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorisationRequest {

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
	protected void performAuthorizationRequest() {
		eventLog.startBlock("Swapping to client_id for second client, but with JWKS and MTLS settings for first client");
		env.mapKey("client", "client2");

		super.performAuthorizationRequest();

		env.unmapKey("client");
		eventLog.endBlock();
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
	protected void cleanUpPingTestResources() {
		unregisterClient1();

		unregisterClient2();
	}
}
