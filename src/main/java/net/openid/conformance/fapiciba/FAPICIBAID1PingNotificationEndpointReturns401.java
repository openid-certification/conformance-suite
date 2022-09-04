package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-backchannel-notification-endpoint-response-401",
	displayName = "FAPI-CIBA-ID1: Ping mode - backchannel notificatione endpoint returns a HTTP 401 Unauthorized response",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 401 Unauthorized response and the authentication flow must still complete normally.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = CIBAMode.class, values = { "poll" })
public class FAPICIBAID1PingNotificationEndpointReturns401 extends AbstractFAPICIBAID1 {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		super.handlePingCallback(requestParts);
		return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 401 Unauthorized response, even though the token is valid.", HttpStatus.UNAUTHORIZED);
	}

}
