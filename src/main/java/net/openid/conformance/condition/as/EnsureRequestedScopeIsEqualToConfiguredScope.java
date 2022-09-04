package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestedScopeIsEqualToConfiguredScope extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "scope", required = "client")
	public Environment evaluate(Environment env) {

		String configuredScope = env.getString("client", "scope");
		String requestedScope = env.getString("scope");

		if (Strings.isNullOrEmpty(configuredScope)) {
			throw error("Missing scope value in client configuration");
		} else {
			if(configuredScope.equals(requestedScope)) {
				logSuccess("Requested scopes match configured scopes", args("scope", configuredScope));
				return env;
			} else {
				throw error("Requested scopes don't match configured scopes",
					args("configured", configuredScope, "requested", requestedScope));
			}
		}
	}

}
