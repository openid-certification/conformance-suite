package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-authorization-request-with-multiple-hints-fails-with-mtls",
	displayName = "FAPI-CIBA: Poll mode - try sending two hints value to authorization endpoint request, should return an error (MTLS client authentication)",
	summary = "This test should return an error that try sending two hints value to authorization endpoint request",
	profile = "FAPI-CIBA",
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
public class FAPICIBAPollEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS extends AbstractFAPICIBAEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS {

	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Variant(name = FAPICIBA.variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		// FIXME: add other variants
		super.setupOpenBankingUkPollMTLS();
	}

	@Override
	protected void cleanupAfterBackchannelRequestShouldHaveFailed() {
		pollCleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}

}
