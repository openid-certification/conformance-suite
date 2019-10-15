package net.openid.conformance.condition.client;

public class EnsureErrorTokenEndpointInvalidRequest extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String getExpectedError() {
		return "invalid_request";
	}
}
