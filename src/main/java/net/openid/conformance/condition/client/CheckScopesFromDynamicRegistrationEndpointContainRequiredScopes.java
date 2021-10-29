package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Set;

public class CheckScopesFromDynamicRegistrationEndpointContainRequiredScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String grantedScope = getStringFromEnvironment(env, "dynamic_registration_endpoint_response", "body_json.scope", "scope in dynamic registration response");
		String requiredScope = getStringFromEnvironment(env, "config", "client.scope", "scope in client configuration");

		Set<String> scopesGranted = Set.of(grantedScope.split(" "));
		Set<String> scopesRequired = Set.of(requiredScope.split(" "));
		ArrayList<String> missing = new ArrayList<>();
		for(String scopeValue : scopesRequired) {
			if(!scopesGranted.contains(scopeValue)) {
				missing.add(scopeValue);
			}
		}

		if (!missing.isEmpty()) {
			throw error("'scope' in dynamic registration response does not contain the required scopes as specified in the test configuration.",
				args("granted", grantedScope, "required", requiredScope, "missing", missing));
		}

		logSuccess("'scope' in dynamic registration response contains the scopes specified in the test configuration.",
			args("granted", grantedScope, "required", requiredScope));

		return env;
	}
}
