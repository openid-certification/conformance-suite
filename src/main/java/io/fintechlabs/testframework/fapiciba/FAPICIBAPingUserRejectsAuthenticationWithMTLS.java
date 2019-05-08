package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.CheckAuthReqIdInCallback;
import io.fintechlabs.testframework.condition.as.CheckNotificationCallbackOnlyAuthReqId;
import io.fintechlabs.testframework.condition.as.VerifyBearerTokenHeaderCallback;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CreateLongRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.WaitForSuccessfulCibaAuthentication;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTls12;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTlsSecureCipher;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-user-rejects-authentication-with-mtls",
	displayName = "FAPI-CIBA: user rejects authentication in Ping mode (MTLS client authentication)",
	summary = "This test requires the user to reject the authentication on their device, for example by pressing the 'cancel' button on the login screen. It verifies the error is correctly notified back to the relying party.",
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
public class FAPICIBAPingUserRejectsAuthenticationWithMTLS extends AbstractFAPICIBAUserRejectAuthentication {

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		// for Ping mode:
		callAndStopOnFailure(WaitForSuccessfulCibaAuthentication.class);

		setStatus(TestModule.Status.WAITING);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		String envKey = "notification_callback";

		eventLog.startBlock(currentClientString() + "Verify notification callback");

		env.putObject(envKey, requestParts);

		env.mapKey("client_request", envKey);

		callAndContinueOnFailure(EnsureIncomingTls12.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, ConditionResult.FAILURE, "FAPI-R-7.1-1");

		env.unmapKey("client_request");

		callAndStopOnFailure(VerifyBearerTokenHeaderCallback.class, "CIBA-10.2");

		callAndStopOnFailure(CheckAuthReqIdInCallback.class, ConditionResult.FAILURE, "CIBA-10.2");

		callAndStopOnFailure(CheckNotificationCallbackOnlyAuthReqId.class, "CIBA-10.2");
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + "Calling token endpoint after ping notification");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();

		int httpStatus = env.getInteger("token_endpoint_response_http_status");

		if (httpStatus == 200) {
			fireTestFailure();
			throw new TestFailureException(new ConditionError(getId(), "Expect user to deny authentication instead of allowing authentication"));
		}

		env.putObject("callback_params", env.getObject("token_endpoint_response"));
		verifyTokenEndpointResponseIsAccessDenied();
		fireTestFinished();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");
	}
}
