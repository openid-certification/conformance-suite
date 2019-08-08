package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CreateInvalidCIBANotificationEndpointUri;
import io.fintechlabs.testframework.condition.client.ServerCalledInvalidNotificationEndpoint;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.Variant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;

@PublishTestModule(
	testName = "fapi-ciba-ping-backchannel-notification-endpoint-return-redirect-request",
	displayName = "FAPI-CIBA: Ping mode - backchannel notification endpoint returns a redirect request response and HTTP 301 Moved_Permanently status",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 301 Moved_Permanently status and a redirect request. The server must not follow redirect request, if it does then test will fail",
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
public class FAPICIBAPingNotificationEndpointReturnsRedirectRequest extends AbstractFAPICIBA {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		super.setupOpenBankingUkPingMTLS();
	}

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

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

		callAndContinueOnFailure(CreateInvalidCIBANotificationEndpointUri.class, "CIBA-10.2");

		String redirectUri = env.getString("invalid_notification_uri");

		try {
			URI redirectEndpoint = new URI(redirectUri);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setLocation(redirectEndpoint);

			setStatus(Status.WAITING);

			return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
		} catch (URISyntaxException e) {
			throw new TestFailureException(getId(), String.format("Couldn't parse %s as URI", redirectUri));
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("invalid-ciba-notification-endpoint")) {

			callAndContinueOnFailure(ServerCalledInvalidNotificationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

			return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}
}
