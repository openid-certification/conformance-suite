package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.fapi.FAPIRWID2;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-ensure-response-mode-query-with-mtls",
	displayName = "FAPI-RW-ID2-OB: ensure response_mode query (with MTLS authentication)",
	summary = "This test includes response_mode=query in the authorization request. The authorization server should show an error message that response_mode=query is not allowed when response_type is 'code id_token' (a screenshot of which should be uploaded), should return an error to the client, or must successfully authenticate.",
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
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	},
	notApplicableForVariants = {
		FAPIRWID2.variant_mtls,
		FAPIRWID2.variant_privatekeyjwt,
		FAPIRWID2.variant_openbankinguk_privatekeyjwt
	}
)
public class FAPIRWID2OBEnsureResponseModeQueryWithMTLS extends AbstractFAPIRWID2OBEnsureResponseModeQuery {

	@Variant(name = variant_openbankinguk_mtls)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Override
	protected void createClientCredentialsRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		// Nothing to do here
	}
}
