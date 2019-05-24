package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.SleepFor5Seconds;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ciba-notification-endpoint-response-401-and-server-does-not-retry-the-call",
	displayName = "FAPI-CIBA: Ping mode (MTLS client authentication) - ciba-notification-endpoint returned a HTTP 401 Unauthorized response, the server does not call the token_endpoint.",
	summary = "This test should process a unsuccessful PING mode flow. The ciba-notification-endpoint will return a HTTP 401 Unauthorized response, the server should not attempt to make the call again thereafter.",
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
public class FAPICIBAPingNotificationEndpointReturns401AndServerDoesNotRetryTheCall extends FAPICIBAPingWithMTLS {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		getTestExecutionManager().runInBackground(() -> {
			callAndStopOnFailure(SleepFor5Seconds.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				//As the server hasn't called the ping endpoint after 5 seconds, assume it has correctly detected the error and aborted.
				fireTestFinished();
			}

			return "done";
		});

		return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 401 Unauthorized response, even though the token is valid.", HttpStatus.UNAUTHORIZED);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		processPingNotificationCallback(requestParts);

		handleSuccessfulTokenEndpointResponse();
	}

}
