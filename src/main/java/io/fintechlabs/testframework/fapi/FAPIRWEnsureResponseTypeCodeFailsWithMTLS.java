package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-ensure-response-type-code-fails-with-mtls",
	displayName = "FAPI-RW: ensure response_type code fails (with MTLS authentication)",
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
public class FAPIRWEnsureResponseTypeCodeFailsWithMTLS extends AbstractFAPIRWEnsureResponseTypeCodeFails {

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
