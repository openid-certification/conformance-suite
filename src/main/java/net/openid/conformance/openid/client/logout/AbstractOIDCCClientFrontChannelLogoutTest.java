package net.openid.conformance.openid.client.logout;

public abstract class AbstractOIDCCClientFrontChannelLogoutTest extends AbstractOIDCCClientLogoutTest {


	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedFrontChannelLogoutCompletedCallback) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected Object createEndSessionEndpointResponse() {
		createFrontChannelLogoutRequestUrl();
		return createFrontChannelLogoutModelAndView(false);
	}


}
