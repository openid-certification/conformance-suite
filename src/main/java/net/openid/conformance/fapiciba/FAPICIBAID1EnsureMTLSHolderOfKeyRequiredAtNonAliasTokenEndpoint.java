package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.SetTokenEndpointToNonMtlsTokenEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-mtls-holder-of-key-required-at-non-alias-token-endpoint",
	displayName = "FAPI-CIBA-ID1: ensure mtls holder of key required at the token endpoint outside mtls_endpoint_aliases",
	summary = "This test ensures that the token_endpoint outside mtls_endpoint_aliases returns an error if a valid request is sent without a TLS certificate. The test is skipped if the server does not publish a separate token endpoint in mtls_endpoint_aliases, as the token endpoint is then covered by fapi-ciba-id1-ensure-mtls-holder-of-key-required.",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1EnsureMTLSHolderOfKeyRequiredAtNonAliasTokenEndpoint extends FAPICIBAID1EnsureMTLSHolderOfKeyRequired {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String mtlsTokenEndpoint = env.getString("server", "mtls_endpoint_aliases.token_endpoint");
		if (mtlsTokenEndpoint == null || mtlsTokenEndpoint.equals(env.getString("server", "token_endpoint"))) {
			fireTestSkipped("The server does not publish a token endpoint in mtls_endpoint_aliases that differs from token_endpoint, so there is no separate token endpoint to test. The token endpoint is covered by fapi-ciba-id1-ensure-mtls-holder-of-key-required.");
			return;
		}

		performAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationResponse() {
		callAndStopOnFailure(SetTokenEndpointToNonMtlsTokenEndpoint.class, "RFC8705-5");
		super.performPostAuthorizationResponse();
	}
}
