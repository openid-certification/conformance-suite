package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-ensure-wrong-client-id-in-token-endpoint-fails-with-mtls",
	displayName = "FAPI-RW-ID2-OB: ensure client id in token endpoint fails (with MTLS authentication)",
	summary = "This test should end with the token endpoint returning an error message that the client is invalid.",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	}
)
public class FAPIRWID2OBEnsureClientIdInTokenEndpointWithMTLS extends AbstractFAPIRWID2OBEnsureClientIdInTokenEndpoint {

	public FAPIRWID2OBEnsureClientIdInTokenEndpointWithMTLS() {
		super(new AbstractFAPIRWID2OBServerTestModule.StepsConfigurationOpenBanking());
	}

	@Override
	protected void createClientCredentialsRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		super.createAuthorizationCodeRequest();
	}
}
