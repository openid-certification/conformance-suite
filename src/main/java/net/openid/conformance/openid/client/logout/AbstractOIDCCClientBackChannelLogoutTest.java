package net.openid.conformance.openid.client.logout;

import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.backchannel_logout_uri"
})
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
