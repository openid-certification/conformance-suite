package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-authorization-request-with-binding-message-succeeds-with-mtls",
	displayName = "FAPI-CIBA: Poll mode - test with a binding message of '1234', the server must authenticate successfully (MTLS client authentication)",
	summary = "This test tries sending a binding message of '1234' to authorization endpoint request then the server must authenticate successfully.",
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
public class FAPICIBAPollEnsureAuthorizationRequestWithBindingMessageSucceedsWithMTLS extends AbstractFAPICIBAEnsureAuthorizationRequestWithBindingMessageSucceedsWithMTLS {

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		waitForPollingAuthenticationToComplete(delaySeconds);
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
