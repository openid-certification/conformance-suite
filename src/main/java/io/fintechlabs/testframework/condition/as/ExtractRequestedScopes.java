package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractRequestedScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		String scope = env.getString("authorization_endpoint_request", "params.scope");

		if (Strings.isNullOrEmpty(scope)) {
			throw error("Missing scope parameter");
		} else {
			logSuccess("Requested scopes", args("scope", scope));

			env.putString("scope", scope);

			return env;
		}

	}

}
