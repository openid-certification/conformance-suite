package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddRequestedExp10sToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusNot200;
import net.openid.conformance.condition.client.SleepUntilAuthReqExpires;
import net.openid.conformance.condition.client.TellUserToIgnoreCIBAAuthentication;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;

@PublishTestModule(
	testName = "fapi-ciba-id1-auth-req-id-expired",
	displayName = "FAPI-CIBA-ID1: user fails to authenticate",
	summary = "This test should end with the token endpoint returning an expired_token error. The user MUST NOT authenticate. requested_expiry is used to request a 10 second expiration time for the authentication request.",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1AuthReqIdExpired extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		// Request a 10 second expiry so the expiry wait is short. 10s safely exceeds the pre-expiry
		// flow (two "expecting pending" polls, the second at ~5s in), so the auth_req_id is still valid
		// when those run; the AS's expiry clock starts at issuance and we only begin the expiry sleep
		// ~6s later, so we always wake well past expiry. (If the server ignores requested_expiry - e.g.
		// Brazil - the test just waits the server's default, as before.)
		callAndStopOnFailure(AddRequestedExp10sToAuthorizationEndpointRequest.class, "CIBA-11");
	}

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior: Don't call automated endpoint, the user must not try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		callAndStopOnFailure(TellUserToIgnoreCIBAAuthentication.class);

		// Sleep conditions release the lock during sleep, allowing incoming
		// ping notifications to be processed.
		callAndStopOnFailure(SleepUntilAuthReqExpires.class);
		if (testType == CIBAMode.PING) {
			// a ping notification may or may not be issued; allow an extra 5 seconds to make sure any ping arrives
			// before we continue
			callAndStopOnFailure(WaitFor5Seconds.class);
		}

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
