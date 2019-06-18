package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.client.CallAutomatedCibaApprovalEndpoint;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.TellUserToRejectCIBAAuthentication;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-user-rejects-authentication",
	displayName = "FAPI-CIBA: user rejects authentication",
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
public class FAPICIBAUserRejectsAuthentication extends AbstractFAPICIBA {

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		super.setupOpenBankingUkPingMTLS();
	}

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		super.setupOpenBankingUkPollMTLS();
	}

	@Variant(name = variant_openbankinguk_poll_privatekeyjwt)
	public void setupOpenBankingUkPollPrivateKeyJwt() {
		super.setupOpenBankingUkPollPrivateKeyJwt();
	}

	@Override
	protected void callAutomatedEndpoint() {
		env.putString("request_action", "deny");
		callAndStopOnFailure(CallAutomatedCibaApprovalEndpoint.class);
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		callAndStopOnFailure(TellUserToRejectCIBAAuthentication.class);

		if (testType == TestType.PING) {
			// test resumes when notification endpoint called
			setStatus(Status.WAITING);
			return;
		}

		int attempts = 0;
		while (attempts++ < 20) {
			setStatus(TestModule.Status.WAITING);
			try {
				Thread.sleep(delaySeconds * 1000);
			} catch (InterruptedException e) {
				throw new TestFailureException(getId(), "Thread.sleep threw exception: " + e.getMessage());
			}
			setStatus(TestModule.Status.RUNNING);

			eventLog.startBlock(currentClientString() + "Polling token endpoint waiting for user to reject authentication");
			callTokenEndpointForCibaGrant();
			eventLog.endBlock();

			callAndStopOnFailure(CheckTokenEndpointHttpStatusNot200.class);

			String error = env.getString("token_endpoint_response", "error");
			if (error.equals("access_denied")) {

				verifyTokenEndpointResponseIsAccessDeniedAndFinishTest();
				return;
			}

			verifyTokenEndpointResponseIsPendingOrSlowDown();

			if (delaySeconds < 60) {
				delaySeconds *= 1.5;
			}
		}

		fireTestFailure();
		throw new TestFailureException(new ConditionError(getId(), "User did not reject authentication before timeout"));
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		if (testType == TestType.PING) {
			processPingNotificationCallback(requestParts);
			verifyTokenEndpointResponseIsAccessDeniedAndFinishTest();
		} else {
			super.processNotificationCallback(requestParts);
		}
	}

	protected void verifyTokenEndpointResponseIsAccessDeniedAndFinishTest() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response is access_denied");

		checkStatusCode400AndValidateErrorFromTokenEndpointResponse();

		env.putObject("callback_params", env.getObject("token_endpoint_response"));
		callAndStopOnFailure(ExpectAccessDeniedErrorFromAuthorizationEndpoint.class);

		eventLog.endBlock();
		fireTestFinished();
	}


}
