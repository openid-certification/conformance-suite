package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddIatValueIsHourInFutureToRequestObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ensure-request-object-iat-is-hour-in-future-fails",
	displayName = "FAPI-CIBA: Ping mode - 'iat' value in request object is 1 hour in the future, should return an error (MTLS client authentication)",
	summary = "This test should return an error that the 'iat' value in request object from back channel authentication endpoint request is 1 hour in the future",
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
public class FAPICIBAPingWithMTLSEnsureRequestObjectIatIsHourInFutureFails extends AbstractFAPICIBAWithMTLSEnsureRequestObjectFails {
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

		callAndStopOnFailure(AddIatValueIsHourInFutureToRequestObject.class, "CIBA-7.1.1");

	}

}
