package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-request-object-signature-algorithm-is-none-fails-with-mtls",
	displayName = "FAPI-CIBA: Poll mode ensure request_object signature algorithm is none fails (MTLS client authentication)",
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
public class FAPICIBAPollEnsureRequestObjectSignatureAlgorithmIsNoneFailsWithMTLS extends AbstractFAPICIBAEnsureRequestObjectSignatureAlgorithmIsNoneFailsWithMTLS {
	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupPollMTLS() {
		// FIXME: add other variants
		super.setupPollMTLS();
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
