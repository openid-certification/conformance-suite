package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-backchannel-notification-endpoint-response-403",
	displayName = "FAPI-CIBA-ID1: Ping mode - backchannel notificatione endpoint returns a HTTP 403 Forbidden response",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 403 Forbidden and the authentication flow must still complete normally.",
	profile = "FAPI-CIBA-ID1",
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
		FAPICIBAID1.variant_poll_mtls,
		FAPICIBAID1.variant_poll_privatekeyjwt,
		FAPICIBAID1.variant_openbankinguk_poll_mtls,
		FAPICIBAID1.variant_openbankinguk_poll_privatekeyjwt
	}
)
// FIXME: mark test as Ping specific
public class FAPICIBAID1PingNotificationEndpointReturns403 extends AbstractFAPICIBAID1 {

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
		return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 403 Forbidden response error, even though the token is valid.", HttpStatus.FORBIDDEN);
	}

}
