package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-backchannel-notification-endpoint-response-has-body",
	displayName = "FAPI-CIBA-ID1: Ping mode - backchannel notification-endpoint returns HTTP 200 OK response with a body",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 200 OK response with a body, the token endpoint should then return successfully as normal. If the token endpoint does not return success, this test will fail with a warning.",
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
public class FAPICIBAID1PingNotificationEndpointReturnsABody extends AbstractFAPICIBAID1 {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		super.handlePingCallback(requestParts);
		return new ResponseEntity<Object>("Backchannel Notification Endpoint returns a HTTP 200 OK response with a body.", HttpStatus.OK);
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
