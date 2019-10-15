package net.openid.conformance.condition.client;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected String getExpectedError() {
		return "invalid_request";
	}
}
