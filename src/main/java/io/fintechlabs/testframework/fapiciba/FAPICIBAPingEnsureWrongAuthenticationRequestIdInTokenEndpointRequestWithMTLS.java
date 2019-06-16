package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ping-ensure-wrong-auth-req-id-in-token-endpoint-request-with-mtls",
	displayName = "FAPI-CIBA: Ping mode ensure wrong auth_req_id in token endpoint request (MTLS client authentication)",
	summary = "This test passes the clinent_2's information (e.g client_id, client_jwks, client_mutual_tls_authentication) and the client_1's auth_req_id in the token endpoint parameters to the one inside the request. The token endpoint server returned an error message that the grant permission is invalid.",
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
public class FAPICIBAPingEnsureWrongAuthenticationRequestIdInTokenEndpointRequestWithMTLS extends AbstractFAPICIBAEnsureWrongAuthenticationRequestIdInTokenEndpointRequestWithMTLS {
	@Variant(name = FAPICIBA.variant_ping_mtls)
	public void setupPingMTLS() {
		// FIXME: add other variants
		super.setupPingMTLS();
	}

	@Override
	protected void performPostAuthorizationResponse() {
		super.performPostAuthorizationResponse();

		callAutomatedEndpoint();
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
