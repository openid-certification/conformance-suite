package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-ensure-client-id-in-token-endpoint-with-mtls",
	displayName = "FAPI-RW: ensure client_id in token endpoint (MTLS authentication)",
	summary = "This test should end with the token endpoint returning an error message that the client is invalid.",
	profile = "FAPI-RW",
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
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class FAPIRWEnsureClientIdInTokenEndpointWithMTLS extends AbstractFAPIRWEnsureClientIdInTokenEndpoint {

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		super.createAuthorizationCodeRequest();
	}
}
