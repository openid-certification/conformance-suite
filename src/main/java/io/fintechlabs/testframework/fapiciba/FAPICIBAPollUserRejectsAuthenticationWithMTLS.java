package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-user-rejects-authentication-with-mtls",
	displayName = "FAPI-CIBA: user rejects authentication in Poll mode (with MTLS authentication)",
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
public class FAPICIBAPollUserRejectsAuthenticationWithMTLS extends AbstractFAPICIBAUserRejectAuthentication {
	@Variant(name = "mtls")
	public void setupMTLS() {
		// FIXME: add private key variant
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {

		int attempts = 0;
		while (attempts++ < 20) {
			setStatus(TestModule.Status.WAITING);
			try {
				Thread.sleep(delaySeconds * 1000);
			} catch (InterruptedException e) {
				throw new TestFailureException(getId(), "Thread.sleep threw exception: " + e.getMessage());
			}
			setStatus(TestModule.Status.RUNNING);

			eventLog.startBlock(currentClientString() + "Polling token endpoint waiting for user to authenticate");
			callTokenEndpointForCibaGrant();
			eventLog.endBlock();

			callAndStopOnFailure(CheckTokenEndpointHttpStatusNot200.class);

			String error = env.getString("token_endpoint_response", "error");
			if (error.equals("access_denied")) {

				verifyTokenEndpointResponseIsAccessDenied();
				fireTestFinished();
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
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}
}
