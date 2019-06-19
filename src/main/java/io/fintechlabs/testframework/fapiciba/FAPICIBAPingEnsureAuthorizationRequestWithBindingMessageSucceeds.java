package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateLongRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.TellUserToDoCIBAAuthentication;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ping-ensure-authorization-request-with-binding-message-succeeds-with-mtls",
	displayName = "FAPI-CIBA: Ping mode - test with a binding message of '1234', the server must authenticate successfully (MTLS client authentication)",
	summary = "This test tries sending a binding message of '1234' to authorization endpoint request then the server must authenticate successfully.",
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
public class FAPICIBAPingEnsureAuthorizationRequestWithBindingMessageSucceeds extends AbstractFAPICIBAEnsureAuthorizationRequestWithBindingMessageSucceeds {

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
	protected void waitForAuthenticationToComplete(long delaySeconds) {

		callAndStopOnFailure(TellUserToDoCIBAAuthentication.class);

		setStatus(Status.WAITING);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		processPingNotificationCallback(requestParts);

		handleSuccessfulTokenEndpointResponse();
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
