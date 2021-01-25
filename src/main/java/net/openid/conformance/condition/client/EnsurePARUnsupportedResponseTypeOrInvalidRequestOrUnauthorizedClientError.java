package net.openid.conformance.condition.client;

public class EnsurePARUnsupportedResponseTypeOrInvalidRequestOrUnauthorizedClientError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	@Override
	protected String[] getExpectedError() {
		return new String[]{
			"unsupported_response_type",
			"invalid_request",
			"unauthorized_client"
		};
	}
}
