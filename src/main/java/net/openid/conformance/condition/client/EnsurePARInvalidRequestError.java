package net.openid.conformance.condition.client;

public class EnsurePARInvalidRequestError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	private static final String HTTP_INVALID_REQUEST = "invalid_request";
	@Override
	protected String getExpectedError() {
		return HTTP_INVALID_REQUEST;
	}
}
