package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnsurePoppyGrantedScopeNotWiderThanRequested extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client", "token_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonElement granted = env.getElementFromObject("token_endpoint_response", "scope");
		if (granted == null) {
			logSuccess("The token response has no 'scope', so the granted scope is identical to the requested scope (RFC 6749 section 5.1)");
			return env;
		}
		if (!granted.isJsonPrimitive() || !granted.getAsJsonPrimitive().isString()) {
			throw error("The 'scope' in the token response is not a string", args("scope", granted));
		}
		String grantedScope = OIDFJSON.getString(granted);
		Set<String> grantedSet = split(grantedScope);
		if (grantedSet.isEmpty()) {
			throw error("The token response 'scope' is empty. Section 4.7 requires the Company to grant at least one scope.",
				args("scope", grantedScope));
		}

		String requestedScope = env.getString("client", "scope");
		Set<String> requested = split(requestedScope == null ? "" : requestedScope);
		List<String> extra = new ArrayList<>();
		for (String s : grantedSet) {
			if (!requested.contains(s)) {
				extra.add(s);
			}
		}
		if (!extra.isEmpty()) {
			throw error("The token response grants scopes that were not requested: " + String.join(" ", extra),
				args("requested", requestedScope, "granted", grantedScope, "extra", extra));
		}
		logSuccess("The granted scope is not wider than the requested scope", args("requested", requestedScope, "granted", grantedScope));
		return env;
	}

	private Set<String> split(String scope) {
		Set<String> set = new HashSet<>(Arrays.asList(scope.trim().split("\\s+")));
		set.remove("");
		return set;
	}
}
