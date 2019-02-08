package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ob-deprecated-eol-sept-2019-ensure-matls-required-code-id-token-with-mtls",
	displayName = "OB-deprecated-EOL-sept-2019: ensure MATLS required (code id_token with MTLS authentication)",
	summary = "This test ensures that all endpoints comply with the TLS version/cipher limitations and that the token endpoint returns an error if a valid request is sent without a TLS certificate.",
	profile = "OB-deprecated-EOL-Sept-2019",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	}
)
public class OBEnsureMATLSRequiredCodeIdTokenWithMTLS extends AbstractOBEnsureMATLSRequiredCodeIdToken {

	@Override
	protected void createClientCredentialsRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
