package net.openid.conformance.condition.client;

public class CheckErrorFromTokenEndpointResponseErrorInvalidClient extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String getExpectedError() {
		return "invalid_client";
	}
}
