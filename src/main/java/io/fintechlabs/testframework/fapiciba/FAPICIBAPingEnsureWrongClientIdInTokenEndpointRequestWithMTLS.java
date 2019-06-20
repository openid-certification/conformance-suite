package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ping-ensure-wrong-client-id-in-token-endpoint-request-with-mtls",
	displayName = "FAPI-CIBA: Ping mode ensure wrong client_id in token endpoint request (MTLS client authentication)",
	summary = "This test should end with the token endpoint server returning an error message that the client is invalid.",
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
public class FAPICIBAPingEnsureWrongClientIdInTokenEndpointRequestWithMTLS extends AbstractFAPICIBAEnsureWrongClientIdInTokenEndpointRequestWithMTLS {

	@Variant(name = FAPICIBA.variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = FAPICIBA.variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		// FIXME: add other variants
		super.setupOpenBankingUkPingMTLS();
	}

	@Override
	protected void performPostAuthorizationResponse() {
		super.performPostAuthorizationResponse();

		callAutomatedEndpoint();

		setStatus(Status.WAITING);
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}

	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
