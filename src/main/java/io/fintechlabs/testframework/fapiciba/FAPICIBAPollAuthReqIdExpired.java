package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddRequestedExp30sToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.condition.client.SleepUntilAuthReqExpires;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-auth-req-id-expired",
	displayName = "FAPI-CIBA: Poll mode - user fails to authenticate",
	summary = "This test should end with the token endpoint returning an expired_token error. The user MUST NOT authenticate. requested_expiry is used to request a 30 second expiration time for the authentication request.",
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
public class FAPICIBAPollAuthReqIdExpired extends AbstractFAPICIBA {
	@Variant(name = "mtls")
	public void setupMTLS() {
		addBackchannelClientAuthentication = AddMTLSClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
	}

	@Variant(name = "private-key-jwt-and-mtls-holder-of-key")
	public void setupPrivateKeyJwt() {
		addBackchannelClientAuthentication = AddPrivateKeyJWTClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior: Don't call automated endpoint, the user must not try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		setStatus(Status.WAITING);
		callAndStopOnFailure(SleepUntilAuthReqExpires.class);
		setStatus(Status.RUNNING);

		eventLog.startBlock(currentClientString() + "Calling token endpoint expecting a token expired error");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();

		callAndStopOnFailure(CheckTokenEndpointHttpStatusNot200.class);

		verifyTokenEndpointResponseIsTokenExpired();
		fireTestFinished();
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExp30sToAuthorizationEndpointRequest.class, "CIBA-11");
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		// Nothing extra to setup for Poll
	}
}
