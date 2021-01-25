package net.openid.conformance.condition.client;

public class EnsurePARInvalidRequestError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	@Override
	protected String[] getExpectedError() {
		return new String[]{"invalid_request"};
	}
}
