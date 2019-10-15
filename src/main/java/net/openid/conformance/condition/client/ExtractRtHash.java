package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRtHash extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "rt_hash")
	public Environment evaluate(Environment env) {
		String hashName = "rt_hash";

		env.removeObject(hashName);

		if (!env.containsObject("id_token")) {
			throw error("Couldn't find parsed ID token");
		}

		String hash = env.getString("id_token", "claims.urn:openid:params:jwt:claim:rt_hash");
		if (hash == null) {
			throw error("Couldn't find urn:openid:params:jwt:claim:rt_hash claim in the ID token");
		}

		String alg = env.getString("id_token", "header.alg");
		if (alg == null) {
			throw error("Couldn't find algorithm in ID token header");
		}

		JsonObject outData = new JsonObject();

		outData.addProperty(hashName, hash);
		outData.addProperty("alg", alg);

		env.putObject(hashName, outData);

		logSuccess("Extracted " + hashName + " from ID Token", outData);

		return env;
	}
}
