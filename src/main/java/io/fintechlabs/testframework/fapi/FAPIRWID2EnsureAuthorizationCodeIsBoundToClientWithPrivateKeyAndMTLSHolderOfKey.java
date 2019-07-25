package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-authorization-code-is-bound-to-client-with-private-key-and-mtls-holder-of-key",
	displayName = "FAPI-RW-ID2: ensure authorization code is bound to client (with private key authentication and mtls holder of key)",
	summary = "This test ensures the token endpoint returns an error if a valid authorization code is used with another client's credentials.",
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
	},
	notApplicableForVariants = {
		FAPIRWID2.variant_mtls,
		FAPIRWID2.variant_openbankinguk_mtls,
		FAPIRWID2.variant_openbankinguk_privatekeyjwt
	}
)
public class FAPIRWID2EnsureAuthorizationCodeIsBoundToClientWithPrivateKeyAndMTLSHolderOfKey extends AbstractFAPIRWID2EnsureAuthorizationCodeIsBoundToClient {

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}
}
