package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-wrong-client-id-in-token-endpoint-request-with-mtls",
	displayName = "FAPI-CIBA: Poll mode ensure wrong client_id in token endpoint request (MTLS client authentication)",
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
public class FAPICIBAPollEnsureWrongClientIdInTokenEndpointRequestWithMTLS extends AbstractFAPICIBAEnsureWrongClientIdInTokenEndpointRequestWithMTLS {
	// No private_key_jwt variant for this test, it's MTLS specific
	@Variant(name = "mtls")
	public void setupMTLS() {
		addBackchannelClientAuthentication = AddMTLSClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
	}

	@Override
	protected void performPostAuthorizationResponse() {
		super.performPostAuthorizationResponse();

		fireTestFinished();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		// Nothings to do
	}

}
