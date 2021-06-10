package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddRequestedExp30sToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusNot200;
import net.openid.conformance.condition.client.SleepUntilAuthReqExpires;
import net.openid.conformance.condition.client.TellUserToIgnoreCIBAAuthentication;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;

@PublishTestModule(
	testName = "fapi-ciba-id1-auth-req-id-expired",
	displayName = "FAPI-CIBA-ID1: user fails to authenticate",
	summary = "This test should end with the token endpoint returning an expired_token error. The user MUST NOT authenticate. requested_expiry is used to request a 30 second expiration time for the authentication request.",
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
public class FAPICIBAID1AuthReqIdExpired extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		// request 30 second expiry, as otherwise the rest takes a long time to run
		// (if the server ignores us, the test just takes a long time to complete)
		callAndStopOnFailure(AddRequestedExp30sToAuthorizationEndpointRequest.class, "CIBA-11");
	}

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior: Don't call automated endpoint, the user must not try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		callAndStopOnFailure(TellUserToIgnoreCIBAAuthentication.class);

		setStatus(Status.WAITING);
		callAndStopOnFailure(SleepUntilAuthReqExpires.class);
		if (testType == CIBAMode.PING) {
			// a ping notification may or may not be issued; allow an extra 5 seconds to make sure any ping arrives
			// before we continue
			callAndStopOnFailure(WaitFor5Seconds.class);
		}
		setStatus(Status.RUNNING);

		callTokenEndpointAndFinishTest();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		if (testType == CIBAMode.PING) {
			verifyNotificationCallback(requestParts);
			setStatus(Status.WAITING);
			// test continues when the above sleep/wait completes
		} else {
			super.processNotificationCallback(requestParts);
		}
	}

	private void callTokenEndpointAndFinishTest() {
		eventLog.startBlock(currentClientString() + "Calling token endpoint expecting a token expired error");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();

		callAndStopOnFailure(CheckTokenEndpointHttpStatusNot200.class);

		verifyTokenEndpointResponseIsTokenExpired();
		fireTestFinished();
	}

}
