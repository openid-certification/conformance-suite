package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-wrong-client-id-in-backchannel-authorization-request-with-mtls",
	displayName = "FAPI-CIBA: Poll mode ensure wrong client_id in backchannel authorization request (MTLS client authentication)",
	summary = "This test should end with the backchannel authorization server returning an error message that must be access_denied or invalid_request",
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
public class FAPICIBAPollEnsureWrongClientIdInBackchannelAuthorizationRequestWithMTLS extends AbstractFAPICIBAEnsureWrongClientIdInBackchannelAuthorizationRequestWithMTLS {

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		// Nothing to do
	}
}
