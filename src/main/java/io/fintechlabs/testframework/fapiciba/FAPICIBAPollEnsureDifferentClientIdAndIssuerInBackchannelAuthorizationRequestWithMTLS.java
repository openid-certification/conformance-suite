package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-different-client-id-and-issuer-in-backchannel-authorization-request-with-mtls",
	displayName = "FAPI-CIBA: Poll mode ensure different client_id and issuer in backchannel authorization request (MTLS client authentication)",
	summary = "This test passes a different client_id and issuer in the backchannel authorization parameters to the one inside the signed request object. The backchannel authorisation server returned an error message that the client is invalid.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
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
public class FAPICIBAPollEnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequestWithMTLS extends AbstractFAPICIBAEnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequestWithMTLS {

	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	// No private_key_jwt variant for this test, it's MTLS specific
	@Variant(name = variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		super.setupOpenBankingUkPollMTLS();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}
}
