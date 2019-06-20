package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateLongRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ping-ensure-authorization-request-with-multiple-hints-fails-with-mtls",
	displayName = "FAPI-CIBA: Ping mode - try sending two hints value to authorization endpoint request, should return an error (MTLS client authentication)",
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
public class FAPICIBAPingEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS extends AbstractFAPICIBAEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS {

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
	protected void modeSpecificAuthorizationEndpointRequest() {

		if ( whichClient == 2 ) {
			callAndStopOnFailure(CreateLongRandomClientNotificationToken.class, "CIBA-7.1", "RFC6750-2.1");
		} else {
			callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");
		}

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}

}
