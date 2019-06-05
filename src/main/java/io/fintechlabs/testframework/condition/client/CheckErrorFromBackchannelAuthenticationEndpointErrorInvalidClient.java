package io.fintechlabs.testframework.condition.client;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidClient  extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected String getExpectedError() {
		return "invalid_client";
	}
}
