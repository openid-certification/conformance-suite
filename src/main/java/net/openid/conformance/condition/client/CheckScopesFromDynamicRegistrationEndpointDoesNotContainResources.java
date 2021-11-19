package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class CheckScopesFromDynamicRegistrationEndpointDoesNotContainResources extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String grantedScope = getStringFromEnvironment(env, "dynamic_registration_endpoint_response", "body_json.scope", "scope in dynamic registration response");

		Set<String> scopesGranted = Set.of(grantedScope.split(" "));

		if(scopesGranted.contains("resources")) {
			throw error("'scope' in dynamic registration response contains the 'resources' scope but it should not have been granted.",
				args("granted", grantedScope));
		}

		logSuccess("'scope' in dynamic registration response does not contain the 'resources' scope",
			args("granted", grantedScope));

		return env;
	}
}
