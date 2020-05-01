package net.openid.conformance.openid.client.logout;

import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.frontchannel_logout_uri"
})

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
