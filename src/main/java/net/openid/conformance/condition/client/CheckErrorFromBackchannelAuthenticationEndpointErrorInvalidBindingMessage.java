package net.openid.conformance.condition.client;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected String getExpectedError() {
		return "invalid_binding_message";
	}

}
