package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class ExtractHash extends AbstractCondition {

	public Environment extractHash(Environment env, String hashName, String envName) {

		env.removeObject(envName);

		if (!env.containsObject("id_token")) {
			throw error("Couldn't find parsed ID token");
		}

		String hash = env.getString("id_token", "claims." + hashName);
		if (hash == null) {
			throw error("Couldn't find " + hashName + " in ID token");
		}

		String alg = env.getString("id_token", "header.alg");
		if (alg == null) {
			throw error("Couldn't find algorithm in ID token header");
		}

		JsonObject outData = new JsonObject();

		outData.addProperty(hashName, hash);
		outData.addProperty("alg", alg);

		env.putObject(envName, outData);

		logSuccess("Extracted " + hashName + " from ID Token", outData);

		return env;
	}

}
