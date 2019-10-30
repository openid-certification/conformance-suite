package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CIBANotificationEndpointCalledUnexpectedly;
import net.openid.conformance.condition.client.TellUserToDoCIBAAuthentication;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-multiple-call-to-token-endpoint",
	displayName = "FAPI-CIBA-ID1: Call token endpoint multiple times in a short space of time",
	summary = "This test should end with the token endpoint server showing an error message: authorization_pending or slow_down or invalid_request or 503 Retry later",
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
public class FAPICIBAID1MultipleCallToTokenEndpoint extends AbstractFAPICIBAID1 {

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior. Don't need to call automated endpoint. User doesn't try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		switch (testType) {
			case PING:
				multipleCallToTokenEndpointAndVerifyResponse();

				callAndStopOnFailure(TellUserToDoCIBAAuthentication.class);

				// Try to fulfil the flow when back channel endpoint return status 200
				// https://gitlab.com/openid/conformance-suite/merge_requests/580
				super.callAutomatedEndpoint();

				setStatus(Status.WAITING);

				break;

			case POLL:
				multipleCallToTokenEndpointAndVerifyResponse();

				fireTestFinished();

				break;

			default:
				throw new RuntimeException("unknown testType");
		}
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		switch (testType) {
			case PING:
				fireTestFinished();
				break;

			case POLL:
				callAndContinueOnFailure(CIBANotificationEndpointCalledUnexpectedly.class, Condition.ConditionResult.FAILURE);
				fireTestFinished();
				break;

			default:
				throw new RuntimeException("unknown testType");
		}
	}

}
