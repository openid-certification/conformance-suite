package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateInvalidCIBANotificationEndpointUri;
import net.openid.conformance.condition.client.ServerCalledInvalidNotificationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-backchannel-notification-endpoint-return-redirect-request",
	displayName = "FAPI-CIBA-ID1: Ping mode - backchannel notification endpoint returns a redirect request response and HTTP 301 Moved_Permanently status",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 301 Moved_Permanently status and a redirect request. The server must not follow redirect request, if it does then test will fail",
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
public class FAPICIBAID1PingNotificationEndpointReturnsRedirectRequest extends AbstractFAPICIBAID1 {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		getTestExecutionManager().runInBackground(() -> {

			// Wait for 15 second and then process notification callback as normal
			Thread.sleep(15 * 1000);

			setStatus(Status.RUNNING);

			processNotificationCallback(requestParts);

			return "done";
		});

		setStatus(Status.RUNNING);

		callAndContinueOnFailure(CreateInvalidCIBANotificationEndpointUri.class, Condition.ConditionResult.WARNING, "CIBA-10.2");

		String redirectUri = env.getString("invalid_notification_uri");

		try {
			URI redirectEndpoint = new URI(redirectUri);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setLocation(redirectEndpoint);

			setStatus(Status.WAITING);

			return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
		} catch (URISyntaxException e) {
			throw new TestFailureException(getId(), "Couldn't parse %s as URI".formatted(redirectUri));
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("invalid-ciba-notification-endpoint")) {

			setStatus(Status.RUNNING);

			callAndContinueOnFailure(ServerCalledInvalidNotificationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

			setStatus(Status.WAITING);

			return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}
}
