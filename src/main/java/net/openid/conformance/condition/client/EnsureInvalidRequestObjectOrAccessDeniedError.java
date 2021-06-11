package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;

public class EnsureInvalidRequestObjectOrAccessDeniedError extends AbstractEnsureAuthorizationEndpointError {
	@Override
	protected ImmutableList<String> getPermittedErrors() {
		return ImmutableList.of(
			"invalid_request_object",
			"access_denied");
	}
}
