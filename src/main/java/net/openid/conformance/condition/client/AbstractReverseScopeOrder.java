package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractReverseScopeOrder extends AbstractCondition {
	protected void reverseScope(Environment env, String envKey) {
		JsonObject authorizationEndpointRequest = env.getObject(envKey);

		JsonElement jsonScope = authorizationEndpointRequest.get("scope");
		if (jsonScope == null || jsonScope.equals(new JsonPrimitive(""))) {
			throw error("no scope found");
		}
		String scope = OIDFJSON.getString(jsonScope);

		List<String> scopes = Arrays.asList(scope.split(" "));
		if (scopes.size() < 2) {
			throw error("'scope' in the configuration must contain more than one scope to run this test");
		}

		Collections.reverse(scopes);

		String newScope = String.join(" ", scopes);

		authorizationEndpointRequest.addProperty("scope", newScope);

		log("Reversed order of scopes in " + envKey,
			args("original", scope, "reversed", newScope));
	}
}
