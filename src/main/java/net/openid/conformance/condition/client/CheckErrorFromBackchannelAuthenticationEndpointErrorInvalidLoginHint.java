package net.openid.conformance.condition.client;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected String getExpectedError() {
		return "invalid_login_hint";
	}
}
