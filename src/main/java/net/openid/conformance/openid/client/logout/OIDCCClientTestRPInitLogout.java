package net.openid.conformance.openid.client.logout;

import com.google.common.base.Strings;
import net.openid.conformance.condition.as.logout.EnsureClientHasAtLeastOneOfBackOrFrontChannelLogoutUri;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;

@PublishTestModule(
	testName = "oidcc-client-test-rp-init-logout",
	displayName = "OIDCC: Relying party test, RP initiated logout.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then terminate the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" at this point the conformance suite will " +
		" send a back channel logout request to the RP if only backchannel_logout_uri is set" +
		" or will send a front channel logout request to the RP if only frontchannel_logout_uri is set" +
		" or will send both front and back channel logout requests" +
		" if both backchannel_logout_uri and frontchannel_logout_uri are set," +
		" then the RP is expected to handle post logout URI redirect." +
		" Corresponds to rp-init-logout in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.backchannel_logout_uri",
	"client.frontchannel_logout_uri"
})

/*
 * OIDCCClientTestRPInitLogoutInvalidState and OIDCCClientTestRPInitLogoutNoState extend this class
 * don't forget to update them if you modify this class
 *
 * This test (and tests extending this one) sends both back channel and front channel logout requests
 * at the same time if both endpoints are defined.
 * Python tests were doing the same and confirmed by Filip that this is intended behavior.
 *
 */
public class OIDCCClientTestRPInitLogout extends AbstractOIDCCClientLogoutTest
{
	protected boolean clientHasBackChannelLogoutUri = false;
	protected boolean clientHasFrontChannelLogoutUri = false;

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest) {
			if(clientHasBackChannelLogoutUri && clientHasFrontChannelLogoutUri) {
				if(receivedFrontChannelLogoutCompletedCallback && sentBackChannelLogoutRequest) {
					fireTestFinished();
					return true;
				}
			} else if(clientHasBackChannelLogoutUri) {
				if(sentBackChannelLogoutRequest) {
					fireTestFinished();
					return true;
				}
			} else if(clientHasFrontChannelLogoutUri) {
				if(receivedFrontChannelLogoutCompletedCallback) {
					fireTestFinished();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected Object handleEndSessionEndpointRequest(String requestId) {
		receivedEndSessionRequest = true;
		callAndStopOnFailure(EnsureClientHasAtLeastOneOfBackOrFrontChannelLogoutUri.class);
		clientHasFrontChannelLogoutUri = !Strings.isNullOrEmpty(env.getString("client", "frontchannel_logout_uri"));
		clientHasBackChannelLogoutUri = !Strings.isNullOrEmpty(env.getString("client", "backchannel_logout_uri"));
		if(clientHasBackChannelLogoutUri) {
			//this must be created before the session is actually removed from env
			createLogoutToken();
		}
		Object viewToReturn = super.handleEndSessionEndpointRequest(requestId);
		if(clientHasBackChannelLogoutUri) {
			sendBackChannelLogoutRequest();
		}
		return viewToReturn;
	}


	@Override
	protected Object createEndSessionEndpointResponse() {
		if(clientHasFrontChannelLogoutUri) {
			createFrontChannelLogoutRequestUrl();
			return createFrontChannelLogoutModelAndView(false);
		} else {
			return super.createEndSessionEndpointResponse();
		}
	}

	protected void skipTestIfStateIsOmitted() {
		String state = env.getString("end_session_endpoint_http_request_params", "state");
		if(Strings.isNullOrEmpty(state)) {
			fireTestSkipped("Skipping test due to the optional state parameter not being supplied to the end_session_endpoint");
		}
	}

}
