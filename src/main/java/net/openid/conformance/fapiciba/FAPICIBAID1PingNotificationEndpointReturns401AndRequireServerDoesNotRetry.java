package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ExpectServerDoesNotCallNotificationEndpointTwice;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-with-mtls-ciba-notification-endpoint-response-401-and-require-server-does-not-retry",
	displayName = "FAPI-CIBA-ID1: Ping mode - ciba-notification-endpoint returned a HTTP 401 Unauthorized response, the server does not retry to call the token_endpoint.",
	summary = "The ciba-notification-endpoint will return a HTTP 401 Unauthorized response, the server should not attempt to make the call again thereafter, if it does the test will be failed",
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
public class FAPICIBAID1PingNotificationEndpointReturns401AndRequireServerDoesNotRetry extends AbstractFAPICIBAID1 {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExpectServerDoesNotCallNotificationEndpointTwice.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

		setStatus(Status.WAITING);

		String calledTimes = env.getString("times_server_called_notification_endpoint");
		if(Integer.valueOf(calledTimes) == 1) {


			getTestExecutionManager().runInBackground(() -> {

				// Wait for 5 second and then process notification callback as normal
				Thread.sleep(5 * 1000);

				setStatus(Status.RUNNING);

				processNotificationCallback(requestParts);

				return "done";
			});

			return new ResponseEntity<Object>("CIBA Notification Endpoint returns a HTTP 401 Unauthorized response, even though the token is valid.", HttpStatus.UNAUTHORIZED);
		} else {
			return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
		}
	}
}
