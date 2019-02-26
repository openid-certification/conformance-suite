package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.CheckAuthReqIdInCallback;
import io.fintechlabs.testframework.condition.client.WaitForSuccessfulCibaAuthentication;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTls12;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTlsSecureCipher;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;

// FIXME document this somewhere else:
// for dev with authlete client, forward notification endpoint back to localhost using ssh -R:3590::8443 button.heenan.me.uk
// to authorise, use https://cibasim.authlete.com/authlete/fapidev/ad/1001

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls",
	displayName = "FAPI-CIBA: Ping mode (MTLS client authentication)",
	summary = "This test requires two different clients registered to use MTLS client authentication under the FAPI-CIBA profile for the 'ping' mode. The test authenticates the user twice (using different variations on the authorisation request etc), tests that certificate bound access tokens are implemented correctly. Do not respond to the request until the test enters the 'WAITING' state.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
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
public class FAPICIBAPingWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		// for Ping mode:
		callAndStopOnFailure(WaitForSuccessfulCibaAuthentication.class);

		setStatus(Status.WAITING);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		String envKey = "notification_callback";

		eventLog.startBlock(currentClientString() + "Verify notification callback");

		env.putObject(envKey, requestParts);

		env.mapKey("client_request", envKey);

		callAndContinueOnFailure(EnsureIncomingTls12.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-R-7.1-1");

		env.unmapKey("client_request");

		// FIXME: CIBA-10.2 verify bearer token in incoming request is client_notification_token
		callAndStopOnFailure(CheckAuthReqIdInCallback.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

		// FIXME: CIBA-10.2 verify callback doesn't contain anything other than auth_req_id
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + "Calling token endpoint after ping notification");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();
		handleSuccessfulTokenEndpointResponse();
	}
}
