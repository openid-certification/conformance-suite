package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus200;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ciba-notification-endpoint-response-200-and-token-endpoint-returns-error",
	displayName = "FAPI-CIBA: Ping mode (MTLS client authentication) - ciba-notification-endpoint returned a HTTP 200 OK response. If the token_endpoint produces a successful or unsuccessful response then throw a warning.",
	summary = "This test should process a successful PING mode flow, ciba-notification-endpoint will return a HTTP 200 OK response with a body, the token endpoint should then return a 200 in response to this and verify a success case." +
		"If the token_endpoint should return an unsuccessful response, then this test will catch it and finish with a WARNING.",
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

public class FAPICIBAPingNotificationEndpointReturns200AndTokenEndpointReturnsError extends FAPICIBAPingWithMTLS {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		super.handlePingCallback(requestParts);
		return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 200 OK response with a body.", HttpStatus.OK);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		processPingNotificationCallback(requestParts);

		int httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == 200) {
			handleSuccessfulTokenEndpointResponse();
		} else {
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus200.class, Condition.ConditionResult.WARNING, "CIBA-10.2");
			fireTestFinished();
		}

	}

}
