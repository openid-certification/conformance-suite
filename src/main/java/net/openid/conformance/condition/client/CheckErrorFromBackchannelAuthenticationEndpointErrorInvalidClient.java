package net.openid.conformance.condition.client;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidClient  extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected String getExpectedError() {
		return "invalid_client";
	}
}
