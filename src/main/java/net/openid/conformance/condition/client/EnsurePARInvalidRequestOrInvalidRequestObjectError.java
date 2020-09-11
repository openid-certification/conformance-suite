package net.openid.conformance.condition.client;

public class EnsurePARInvalidRequestOrInvalidRequestObjectError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	@Override
	protected String[] getExpectedError() {
		return new String[]{
			"invalid_request",
			"invalid_request_object"
		};
	}
}
