package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractCompareJwks extends AbstractCondition {
	public Set<JsonObject> filterJsonArrayToSetContainingSigningKeys(JsonArray keys) {
		Set<JsonObject> filtered = new HashSet<>();

		keys.forEach(keyJsonElement -> {
			JsonObject keyObject = keyJsonElement.getAsJsonObject();

			JsonPrimitive use = keyObject.getAsJsonPrimitive("use");

			if (use == null || OIDFJSON.getString(use).equals("sig")) {
				// 'use' attribute is completely optional, so we include use: sig or no use claim
				filtered.add(keyObject);
			}
		});
		return filtered;
	}

	@Override
	@PreEnvironment(required = { "original_jwks", "new_jwks" } )
	public Environment evaluate(Environment env) {
		// This condition would be a lot easier to write/more robust if we knew which key the OP was & now is using
		// to sign id_tokens - but the python version of this test doesn't do an authentication
		JsonArray originalKeys = env.getObject("original_jwks").getAsJsonArray("keys");
		JsonArray newKeys = env.getObject("new_jwks").getAsJsonArray("keys");

		var originalSigningKeys = filterJsonArrayToSetContainingSigningKeys(originalKeys);
		var latestSigningKeys = filterJsonArrayToSetContainingSigningKeys(newKeys);

		compareJwks(originalSigningKeys, latestSigningKeys);

		return env;
	}

	protected abstract void compareJwks(Set<JsonObject> originalSigningKeys, Set<JsonObject> latestSigningKeys);
}
