package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-wrong-client-id-in-backchannel-authorization-request-with-mtls",
	displayName = "FAPI-CIBA: Poll mode ensure wrong client_id in backchannel authorization request (MTLS client authentication)",
	summary = "This test sends the wrong client_id for the MTLS key to the backchannel authorization endpoint, and should end with the server returning an access_denied or invalid_request error",
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
	// No private_key_jwt variant for this test, it's MTLS specific
	@Variant(name = variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		// Nothing to do
	}
}
