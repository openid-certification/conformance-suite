package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.logout.CallRPBackChannelLogoutEndpoint;
import net.openid.conformance.condition.as.logout.EncryptLogoutToken;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutUriResponseStatusCodeIs200;
import net.openid.conformance.condition.as.logout.EnsureClientHasBackChannelLogoutUri;
import net.openid.conformance.condition.as.logout.GenerateLogoutTokenClaims;
import net.openid.conformance.condition.as.logout.OIDCCSignLogoutToken;


public abstract class AbstractOIDCCClientBackChannelLogoutTest extends AbstractOIDCCClientLogoutTest
{
	protected boolean sentBackChannelLogoutRequest = false;



	//TODO this is probably not necessary
	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest && sentBackChannelLogoutRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	protected void createLogoutToken() {
		generateLogoutTokenClaims();
		customizeLogoutTokenClaims();

		signLogoutToken();

		customizeLogoutTokenSignature();

		encryptLogoutTokenIfNecessary();
	}

	protected void generateLogoutTokenClaims() {
		callAndStopOnFailure(GenerateLogoutTokenClaims.class, "OIDCBCL-2.4");
	}

	@Override
	protected Object handleEndSessionEndpointRequest(String requestId)
	{
		//this must be created before the session is actually removed from env
		createLogoutToken();
		waitAndSendLogoutRequest();
		return super.handleEndSessionEndpointRequest(requestId);
	}

	protected void waitAndSendLogoutRequest() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(2 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				sendBackChannelLogoutRequest();
			}
			return "done";
		});
	}

	/**
	 * Test ends here
	 */
	protected void sendBackChannelLogoutRequest() {
		call(exec().startBlock("Sending Back Channel Logout Request"));
		callAndStopOnFailure(EnsureClientHasBackChannelLogoutUri.class, "OIDCBCL-2.2");
		callAndStopOnFailure(CallRPBackChannelLogoutEndpoint.class, "OIDCBCL-2.5");
		validateBackChannelLogoutResponse();
		call(exec().endBlock());
		fireTestFinished();
	}

	protected void validateBackChannelLogoutResponse() {
		callAndContinueOnFailure(EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders.class,
				Condition.ConditionResult.WARNING, "OIDCBCL-2.8");
	}


	/**
	 * Override to modify logout token claims
	 * Called right after generateLogoutTokenClaims
	 */
	protected void customizeLogoutTokenClaims(){

	}

	protected void customizeLogoutTokenSignature(){

	}


	protected void signLogoutToken() {
		callAndContinueOnFailure(OIDCCSignLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
	}

	protected void encryptLogoutTokenIfNecessary() {
		skipIfElementMissing("client", "id_token_encrypted_response_alg", Condition.ConditionResult.INFO,
			EncryptLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4", "OIDCC-10.2");
	}

}
