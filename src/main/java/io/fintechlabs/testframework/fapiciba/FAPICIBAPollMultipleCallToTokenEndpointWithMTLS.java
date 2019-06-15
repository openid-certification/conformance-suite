package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-multiple-call-to-token-endpoint-with-mtls",
	displayName = "FAPI-CIBA: Poll mode - call token endpoint multiple times in a short space of time (MTLS client authentication)",
	summary = "This test should end with the token endpoint server showing an error message: authorization_pending or slow_down or 503 Retry later",
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
public class FAPICIBAPollMultipleCallToTokenEndpointWithMTLS extends AbstractFAPICIBAWithMTLS {
	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupMTLS() {
		// FIXME: add private key variant
	}

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior. Don't need to call automated endpoint. User doesn't try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {

		multipleCallToTokenEndpointAndVerifyResponse();

		fireTestFinished();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}
}
