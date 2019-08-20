package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-refresh-token",
	displayName = "FAPI-RW-ID2: obtain an id token using a refresh token",
	summary = "This test uses a refresh_token to obtain an id token and ensures that claims satisfy the requirements.",
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
public class FAPIRWID2RefreshToken extends AbstractFAPIRWID2RefreshTokenTestModule {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}

	@Variant(
		name = variant_openbankinguk_mtls,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(
		name = variant_openbankinguk_privatekeyjwt,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}
}
