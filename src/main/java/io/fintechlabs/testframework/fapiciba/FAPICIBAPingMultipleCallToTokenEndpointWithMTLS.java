package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.WaitForSuccessfulCibaAuthentication;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-multiple-call-to-token-endpoint-with-mtls",
	displayName = "FAPI-CIBA: Ping mode - call token endpoint multiple times in a short space of time (MTLS client authentication)",
	summary = "This test should end with the token endpoint server showing an error message: authorization_pending or slow_down or 503 Retry later.",
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
public class FAPICIBAPingMultipleCallToTokenEndpointWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior. Don't need to call automated endpoint. User doesn't try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		// for Ping mode:
		callAndStopOnFailure(WaitForSuccessfulCibaAuthentication.class);

		setStatus(Status.WAITING);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		verifyNotificationCallback(requestParts);

		multipleCallToTokenEndpointAndVerifyResponse();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}
}
