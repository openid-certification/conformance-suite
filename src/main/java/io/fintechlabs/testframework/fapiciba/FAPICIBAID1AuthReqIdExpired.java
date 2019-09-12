package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddRequestedExp30sToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.condition.client.SleepUntilAuthReqExpires;
import io.fintechlabs.testframework.condition.client.TellUserToIgnoreCIBAAuthentication;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-id1-auth-req-id-expired",
	displayName = "FAPI-CIBA-ID1: user fails to authenticate",
	summary = "This test should end with the token endpoint returning an expired_token error. The user MUST NOT authenticate. requested_expiry is used to request a 30 second expiration time for the authentication request.",
	profile = "FAPI-CIBA-ID1",
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
public class FAPICIBAID1AuthReqIdExpired extends AbstractFAPICIBAID1 {
	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Variant(name = variant_poll_privatekeyjwt)
	public void setupPollPrivateKeyJwt() {
		super.setupPollPrivateKeyJwt();
	}

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
		if (testType == TestType.PING) {
			// test resumes when notification endpoint called
			return;
		}
		callAndStopOnFailure(SleepUntilAuthReqExpires.class);
		setStatus(Status.RUNNING);

		callTokenEndpointAndFinishTest();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		if (testType == TestType.PING) {
			verifyNotificationCallback(requestParts);
			callTokenEndpointAndFinishTest();
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
		cleanUpPingTestResources();
		fireTestFinished();
	}

}
