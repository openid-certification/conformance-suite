package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-response-mode-query-with-private-key-and-mtls-holder-of-key",
	displayName = "FAPI-RW-ID2: ensure response_mode query (with private key authentication and mtls holder of key)",
	summary = "This test includes response_mode=query in the authorization request. The authorization server should show an error message that response_mode=query is not allowed when response_type is 'code id_token' (a screenshot of which should be uploaded), should return an error to the client, or must successfully authenticate.",
	profile = "FAPI-RW-ID2",
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
public class FAPIRWID2EnsureResponseModeQueryWithPrivateKeyAndMTLSHolderOfKey extends AbstractFAPIRWID2EnsureResponseModeQuery {

	@Override
	protected void createAuthorizationCodeRequest() {
		// Nothing to do here
	}
}
