package net.openid.conformance.condition.client;

public class CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest extends AbstractCheckErrorFromParEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[]{"invalid_request", "invalid_client"};
	}
}
