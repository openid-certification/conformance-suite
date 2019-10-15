package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenNotIncludeCHashAndSHash extends AbstractCondition {

	@Override
	@PreEnvironment( required = "id_token" )
	public Environment evaluate(Environment env) {

		JsonElement claimElement = env.getElementFromObject("id_token", "claims");

		if (claimElement == null || !claimElement.isJsonObject()) {
			log("Skipped to check claims that is null or not a json");
		}

		JsonObject claims = claimElement.getAsJsonObject();

		if (claims.has("s_hash") || claims.has("c_hash")) {
			throw error("claims contains 'c_hash' or 's_hash'", args("claims", claims));
		}

		logSuccess("id_token claims correctly does not contain 'c_hash' and 's_hash'", args("claims", claims));

		return env;
	}
}
