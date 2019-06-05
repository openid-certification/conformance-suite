package io.fintechlabs.testframework.condition.client;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected String getExpectedError() {
		return "invalid_binding_message";
	}

}
