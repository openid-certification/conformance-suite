package net.openid.conformance.openid.client.logout;

public abstract class AbstractOIDCCClientBackChannelLogoutTest extends AbstractOIDCCClientLogoutTest {

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest && sentBackChannelLogoutRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected Object handleEndSessionEndpointRequest(String requestId) {
		//this must be created before the session is actually removed from env
		createLogoutToken();
		Object viewToReturn = super.handleEndSessionEndpointRequest(requestId);
		sendBackChannelLogoutRequest();
		return viewToReturn;
	}

}
