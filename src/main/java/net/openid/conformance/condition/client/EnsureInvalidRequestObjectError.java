package net.openid.conformance.condition.client;

public class EnsureInvalidRequestObjectError extends AbstractEnsureSpecifiedErrorFromAuthorizationEndpointResponse {

	@Override
	protected String getExpectedError() {
		return "invalid_request_object";
	}
}
