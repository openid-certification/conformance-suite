package net.openid.conformance.condition.client;

public class EnsurePARInvalidRequestObjectError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	private static final String HTTP_INVALID_REQUEST_OBJECT = "invalid_request_object";
	@Override
	protected String[] getExpectedError() {
		return new String[]{HTTP_INVALID_REQUEST_OBJECT};
	}
}
