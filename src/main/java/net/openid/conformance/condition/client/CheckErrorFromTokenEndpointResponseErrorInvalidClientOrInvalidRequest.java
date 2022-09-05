package net.openid.conformance.condition.client;

public class CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[]{"invalid_request", "invalid_client"};
	}

}
