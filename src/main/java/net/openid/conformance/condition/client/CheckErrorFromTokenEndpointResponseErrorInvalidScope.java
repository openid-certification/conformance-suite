package net.openid.conformance.condition.client;

public class CheckErrorFromTokenEndpointResponseErrorInvalidScope extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[] { "invalid_scope" };
	}
}
