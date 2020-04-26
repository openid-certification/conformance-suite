package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.logout.CallRPBackChannelLogoutEndpoint;
import net.openid.conformance.condition.as.logout.EncryptLogoutToken;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutUriResponseStatusCodeIs200;
import net.openid.conformance.condition.as.logout.EnsureClientHasBackChannelLogoutUri;
import net.openid.conformance.condition.as.logout.GenerateLogoutTokenClaims;
import net.openid.conformance.condition.as.logout.OIDCCSignLogoutToken;


public abstract class AbstractOIDCCClientBackChannelLogoutTest extends AbstractOIDCCClientLogoutTest {
	protected boolean sentBackChannelLogoutRequest = false;



	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest && sentBackChannelLogoutRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	protected void createLogoutToken() {
		call(exec().startBlock("Creating Logout Token"));
		generateLogoutTokenClaims();
		customizeLogoutTokenClaims();

		signLogoutToken();

		customizeLogoutTokenSignature();

		encryptLogoutTokenIfNecessary();
		call(exec().endBlock());
	}

	protected void generateLogoutTokenClaims() {
		callAndContinueOnFailure(GenerateLogoutTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
	}

	@Override
	protected Object handleEndSessionEndpointRequest(String requestId) {
		//this must be created before the session is actually removed from env
		createLogoutToken();
		Object viewToReturn = super.handleEndSessionEndpointRequest(requestId);
		sendBackChannelLogoutRequest();
		return viewToReturn;
	}

	/**
	 * Test ends here
	 */
	protected void sendBackChannelLogoutRequest() {
		call(exec().startBlock("Sending Back Channel Logout Request"));
		callAndContinueOnFailure(EnsureClientHasBackChannelLogoutUri.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.2");
		callAndContinueOnFailure(CallRPBackChannelLogoutEndpoint.class, Condition.ConditionResult.FAILURE,"OIDCBCL-2.5");
		validateBackChannelLogoutResponse();
		call(exec().endBlock());
		sentBackChannelLogoutRequest=true;
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
