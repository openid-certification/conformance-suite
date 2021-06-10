package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureInvalidRequestInvalidRequestObjectOrInvalidRequestUriError extends AbstractEnsureError {
	@Override
	protected ImmutableList<String> getPermittedErrors() {
		return ImmutableList.of(
			"invalid_request",
			"invalid_request_object",
			"invalid_request_uri");
	}
}
