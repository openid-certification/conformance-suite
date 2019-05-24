package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-poll-multiple-call-to-token-endpoint-with-mtls",
	displayName = "FAPI-CIBA: Poll mode (MTLS client authentication)",
	summary = "This test should end with the token endpoint server showing an error message: authorization_pending or slow_down or 503 Retry later",
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
public class FAPICIBAPollMultipleCallToTokenEndpointWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior. Don't need to call automated endpoint. User doesn't try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {

		multipleCallToTokenEndpointAndVerifyResponse();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		fireTestFailure();
		throw new ConditionError(getId(), "Notification endpoint was called during a poll test");
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}
}
