package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-ping-backchannel-notification-endpoint-response-401",
	displayName = "FAPI-CIBA: Ping mode - backchannel notificatione endpoint returns a HTTP 401 Unauthorized response",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 401 Unauthorized response and the authentication flow must still complete normally.",
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
	},
	notApplicableForVariants = {
		FAPICIBA.variant_poll_mtls,
		FAPICIBA.variant_poll_privatekeyjwt,
		FAPICIBA.variant_openbankinguk_poll_mtls,
		FAPICIBA.variant_openbankinguk_poll_privatekeyjwt
	}
)
public class FAPICIBAPingNotificationEndpointReturns401 extends AbstractFAPICIBA {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() { super.setupOpenBankingUkPingMTLS(); }

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}


	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		super.handlePingCallback(requestParts);
		return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 401 Unauthorized response, even though the token is valid.", HttpStatus.UNAUTHORIZED);
	}

}
