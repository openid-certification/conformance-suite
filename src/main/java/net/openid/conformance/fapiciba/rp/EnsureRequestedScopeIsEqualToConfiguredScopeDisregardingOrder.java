package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestedScopeIsEqualToConfiguredScopeDisregardingOrder extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "scope", required = "client")
	public Environment evaluate(Environment env) {

		String configuredScope = env.getString("client", "scope");
		String requestedScope = env.getString("scope");

		if (Strings.isNullOrEmpty(configuredScope)) {
			throw error("Missing scope value in client configuration");
		} else {

			String[] configuredScopes = configuredScope.split(" ");
			String[] requestedScopes = requestedScope.split(" ");
			boolean scopesAreEqual = ImmutableSet.copyOf(configuredScopes).equals(ImmutableSet.copyOf(requestedScopes));

			if(scopesAreEqual) {
				logSuccess("Requested scopes match configured scopes", args("scope", configuredScope));
				return env;
			} else {
				throw error("Requested scopes don't match configured scopes",
					args("configured", configuredScope, "requested", requestedScope));
			}
		}
	}

}
