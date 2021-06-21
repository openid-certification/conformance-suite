package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;

public class EnsureInvalidRequestInvalidRequestObjectOrInvalidRequestUriError extends AbstractEnsureAuthorizationEndpointError {
	@Override
	protected ImmutableList<String> getPermittedErrors() {
		return ImmutableList.of(
			"invalid_request",
			"invalid_request_object",
			"invalid_request_uri");
	}
}
