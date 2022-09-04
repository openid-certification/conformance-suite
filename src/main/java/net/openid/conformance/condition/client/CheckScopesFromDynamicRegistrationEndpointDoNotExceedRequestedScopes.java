package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Set;

public class CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String grantedScope = getStringFromEnvironment(env, "dynamic_registration_endpoint_response", "body_json.scope", "scope in dynamic registration response");
		String requestedScope = getStringFromEnvironment(env, "dynamic_registration_request", "scope", "scope in dynamic registration request");

		Set<String> scopesGranted = Set.of(grantedScope.split(" "));
		Set<String> scopesRequested = Set.of(requestedScope.split(" "));
		ArrayList<String> notRequested = new ArrayList<>();
		for(String scopeValue : scopesGranted) {
			if(!scopesRequested.contains(scopeValue)) {
				notRequested.add(scopeValue);
			}
		}

		if (!notRequested.isEmpty()) {
			throw error("'scope' in dynamic registration response contains scope(s) that were not requested.",
				args("granted", grantedScope, "requested", requestedScope, "not_requested", notRequested));
		}

		logSuccess("'scope' in dynamic registration response contains only scopes that were requested.",
			args("granted", grantedScope, "requested", requestedScope));

		return env;
	}
}
