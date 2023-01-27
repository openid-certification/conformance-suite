package net.openid.conformance.condition.client;

public class EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	@Override
	protected String[] getExpectedError() {
		return new String[]{
			"invalid_request",
			"invalid_request_object",
			"request_uri_not_supported"
		};
	}
}
