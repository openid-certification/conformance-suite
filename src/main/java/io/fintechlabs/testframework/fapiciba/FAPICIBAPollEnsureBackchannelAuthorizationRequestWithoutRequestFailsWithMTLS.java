package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-backchannel-authorization-request-without-request-fails-with-mtls",
	displayName = "FAPI-CIBA: Poll mode ensure backchannel authorization request without request fails (MTLS client authentication)",
	summary = "This test should end with the backchannel authorisation server returning an error message that the request is invalid.",
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
public class FAPICIBAPollEnsureBackchannelAuthorizationRequestWithoutRequestFailsWithMTLS extends AbstractFAPICIBAEnsureBackchannelAuthorizationRequestWithoutRequestFailsWithMTLS {
	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupMTLS() {
		// FIXME: add private key variant
	}

	@Override
	protected void cleanupAfterBackchannelRequestShouldHaveFailed() {
		pollCleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}
}
