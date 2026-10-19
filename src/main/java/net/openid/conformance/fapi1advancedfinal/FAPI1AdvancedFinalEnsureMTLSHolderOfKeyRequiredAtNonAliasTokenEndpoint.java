package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.SetTokenEndpointToNonMtlsTokenEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-ensure-mtls-holder-of-key-required-at-non-alias-token-endpoint",
	displayName = "FAPI1-Advanced-Final: ensure mtls holder of key required at the token endpoint outside mtls_endpoint_aliases",
	summary = "This test ensures that the token_endpoint outside mtls_endpoint_aliases returns an error if a valid request is sent without a TLS certificate. The test is skipped if the server does not publish a separate token endpoint in mtls_endpoint_aliases, as the token endpoint is then covered by fapi1-advanced-final-ensure-mtls-holder-of-key-required.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
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
public class FAPI1AdvancedFinalEnsureMTLSHolderOfKeyRequiredAtNonAliasTokenEndpoint extends FAPI1AdvancedFinalEnsureMTLSHolderOfKeyRequired {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String mtlsTokenEndpoint = env.getString("server", "mtls_endpoint_aliases.token_endpoint");
		if (mtlsTokenEndpoint == null || mtlsTokenEndpoint.equals(env.getString("server", "token_endpoint"))) {
			fireTestSkipped("The server does not publish a token endpoint in mtls_endpoint_aliases that differs from token_endpoint, so there is no separate token endpoint to test. The token endpoint is covered by fapi1-advanced-final-ensure-mtls-holder-of-key-required.");
			return;
		}

		performAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationFlow() {
		callAndStopOnFailure(SetTokenEndpointToNonMtlsTokenEndpoint.class, "RFC8705-5");
		super.performPostAuthorizationFlow();
	}
}
