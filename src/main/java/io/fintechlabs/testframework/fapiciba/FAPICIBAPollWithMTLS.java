package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.client.AddRequestedExp300SToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-poll-with-mtls",
	displayName = "FAPI-CIBA: Poll mode (MTLS client authentication)",
	summary = "This test requires two different clients registered to use MTLS client authentication under the FAPI-CIBA profile for the 'poll' mode. The test authenticates the user twice (using different variations on the authorisation request etc), tests that certificate bound access tokens are implemented correctly. Do not respond to the request until the test enters the 'WAITING' state.",
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
public class FAPICIBAPollWithMTLS extends AbstractFAPICIBAWithMTLS {

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
		if ( whichClient == 2 ) {
			callAndStopOnFailure(AddRequestedExp300SToAuthorizationEndpointRequest.class, "CIBA-11");
		}
	}
}
