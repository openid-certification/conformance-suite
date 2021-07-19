package net.openid.conformance.condition.client;

public class CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidGrant extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[]{"invalid_client", "invalid_grant"};
	}

}
