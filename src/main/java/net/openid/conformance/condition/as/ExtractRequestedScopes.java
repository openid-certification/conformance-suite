package net.openid.conformance.condition.as;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestedScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		String scope = env.getString("authorization_endpoint_request", "query_string_params.scope");

		if (Strings.isNullOrEmpty(scope)) {
			throw error("Missing scope parameter");
		} else {
			logSuccess("Requested scopes", args("scope", scope));

			env.putString("scope", scope);

			return env;
		}

	}

}
