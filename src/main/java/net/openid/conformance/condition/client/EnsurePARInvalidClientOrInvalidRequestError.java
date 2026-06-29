package net.openid.conformance.condition.client;

public class EnsurePARInvalidClientOrInvalidRequestError extends AbstractCheckErrorFromParEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[]{"invalid_client", "invalid_request"};
	}
}
