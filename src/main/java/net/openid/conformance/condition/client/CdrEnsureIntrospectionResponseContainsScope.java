package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrEnsureIntrospectionResponseContainsScope extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallTokenIntrospectionEndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {

		String scope = env.getString(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json.scope");

		if (Strings.isNullOrEmpty(scope)) {
			throw error("scope is missing from the introspection response; CDR requires it for active tokens",
				args("introspection_response", env.getElementFromObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json")));
		}

		logSuccess("The introspection response contains a scope", args("scope", scope));
		return env;
	}

}
