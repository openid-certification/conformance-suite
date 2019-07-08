package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAlgorithmAsRS256;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus401;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ensure-client-assertion-signature-algorithm-in-token-endpoint-request-is-RS256-fails",
	displayName = "FAPI-CIBA: Ensure client_assertion signature algorithm in token endpoint request is RS256 fails",
	summary = "This test should end with the token endpoint returning an error message that the client is invalid.",
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
	},
	notApplicableForVariants = {
		FAPICIBA.variant_ping_mtls,
		FAPICIBA.variant_poll_mtls,
		FAPICIBA.variant_openbankinguk_ping_mtls,
		FAPICIBA.variant_openbankinguk_poll_mtls
	}
)
public class FAPICIBAEnsureClientAssertionSignatureAlgorithmInTokenEndpointRequestIsRS256Fails extends AbstractFAPICIBA {

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
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus401.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
