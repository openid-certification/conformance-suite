package net.openid.conformance.condition.client;

public class CheckErrorFromTokenEndpointResponseErrorInvalidGrant extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[] { "invalid_grant" };
	}
}
