package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.ClientCalledTokenEndpointAfterReceivingARedirect;
import io.fintechlabs.testframework.condition.client.CreateInvalidRedirectTargetEndpoint;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ciba-notification-endpoint-response-301-and-client-returns-error",
	displayName = "FAPI-CIBA: Ping mode (MTLS client authentication) - ciba-notification-endpoint returned a HTTP 301 Redirect, the server should incorrectly follow this, client should therefore return an error.",
	summary = "This test should process an unsuccessful PING mode flow, ciba-notification-endpoint will return a HTTP 301 Redirect, the server should incorrectly follow this, resulting in the client throwing an error.",
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
public class FAPICIBAPingNotificationEndpointReturns301AndClientReturnsError extends FAPICIBAPingWithMTLS {

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		callAndContinueOnFailure(CreateInvalidRedirectTargetEndpoint.class);

		super.handlePingCallback(requestParts);

		try {
			URI redirectEndpoint = new URI(env.getString("invalid_redirect_target_endpoint_uri"));
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setLocation(redirectEndpoint);
			return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		callAndContinueOnFailure(ClientCalledTokenEndpointAfterReceivingARedirect.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");
		fireTestFinished();
		throw new ConditionError(getId(), "Notification endpoint was incorrectly called after a HTTP Redirect was issued to the server.");
	}

}
