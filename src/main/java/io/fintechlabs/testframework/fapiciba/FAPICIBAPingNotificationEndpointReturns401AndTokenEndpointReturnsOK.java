package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ciba-notification-endpoint-response-401-and-token-endpoint-returns-ok",
	displayName = "FAPI-CIBA: Ping mode (MTLS client authentication) - ciba-notification-endpoint returned a HTTP 401 Unauthorized response, token_endpoint returns OK.",
	summary = "This test should process a successful PING mode flow, ciba-notification-endpoint will return a HTTP 401 Unauthorized response and the token_endpoint returns a HTTP 200 OK response.",
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
public class FAPICIBAPingNotificationEndpointReturns401AndTokenEndpointReturnsOK extends FAPICIBAPingWithMTLS {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		super.handlePingCallback(requestParts);
		return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 401 Unauthorized response, even though the token is valid.", HttpStatus.UNAUTHORIZED);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		processPingNotificationCallback(requestParts);

		handleSuccessfulTokenEndpointResponse();
	}

}
