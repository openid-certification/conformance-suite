package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Use only for refresh requests
 * Overwrites the current scope value in env
 * If scope is not provided, existing scope value (from the authorization request) will continue be used
 */
public class ExtractScopeFromTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String scope = env.getString("token_endpoint_request", "body_form_params.scope");

		if (Strings.isNullOrEmpty(scope)) {
			log("Token endpoint request does not contain a scope parameter");
			return env;
		} else {
			log("Scopes requested in refresh request", args("scope", scope));

			env.putString("scope", scope);

			return env;
		}

	}

}
