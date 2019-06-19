package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddBadIssToRequestObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ensure-request-object-bad-iss-fails",
	displayName = "FAPI-CIBA: Ping mode - bad 'iss' value in request object, should return an error (MTLS client authentication)",
	summary = "This test should return an error that the 'iss' value in request object from back channel authentication endpoint request is incorrect (a valid client_id, but not for the client in use)",
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
public class FAPICIBAPingWithMTLSEnsureRequestObjectBadIssFails extends AbstractFAPICIBAWithMTLSEnsureRequestObjectFails {

	@Variant(name = FAPICIBA.variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = FAPICIBA.variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		// FIXME: add other variants
		super.setupOpenBankingUkPingMTLS();
	}

	@Override
	protected void cleanupAfterBackchannelRequestShouldHaveFailed() {
		pingCleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(AddBadIssToRequestObject.class, "CIBA-7.1.1");
	}

}
