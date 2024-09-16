package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddEncryptionParametersToClientMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "client_public_jwks"})
	public Environment evaluate(Environment env) {

		JsonObject clientMetaData = (JsonObject) env.getElementFromObject("authorization_endpoint_request", "client_metadata");

		JsonObject publicJwks = env.getObject("client_public_jwks");
		JsonArray keys = publicJwks.getAsJsonArray("keys");
		JsonObject encKey = null;

		for (JsonElement jwkEl: keys) {
			JsonObject jwk = jwkEl.getAsJsonObject();
			if (!jwk.has("use")) {
				continue;
			}
			String use = OIDFJSON.getString(jwk.get("use"));
			if (use.equals("enc")) {
				if (encKey != null) {
					throw error("client jwks contains more than one key with 'use': 'enc'", args("clientjwks", publicJwks));
				}
				encKey = jwk;
			}
		}
		if (encKey == null) {
			throw error("The client jwks does not contain a key with 'use': 'enc'", args("clientjwks", publicJwks));
		}
		JsonArray keysArray = new JsonArray();
		keysArray.add(encKey);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keysArray);

		clientMetaData.add("jwks", jwks);

		String alg = env.getString("client", "authorization_encrypted_response_alg");
		String enc = env.getString("client", "authorization_encrypted_response_enc");

		if (alg == null) {
			// get alg from the jwk
			JsonElement algEl = encKey.get("alg");
			if (algEl != null) {
				alg = OIDFJSON.getString(algEl);
			}
		}
		if (alg == null) {
			// use kty to guess at a sensible value based on kty in jwk
			String kty = OIDFJSON.getString(encKey.get("kty"));
			switch (kty) {
				case "RSA":
					alg = "RSA-OAEP";
					break;
				case "EC":
					alg = "ECDH-ES";
					break;
				default:
					// leave as null
			}
		}

		if (alg == null) {
			throw error("An encrypted response is selected, please set authorization_encrypted_response_enc in the test configuration.");
		}


		if (enc == null) {
			switch (alg) {
				case "RSA-OAEP":
					enc = "A128CBC-HS256";
					break;
				case "ECDH-ES":
					enc = "A256GCM";
					break;
				default:
					// leave as null
			}
		}
		if (enc == null) {
			throw error("An encrypted response is selected, please set authorization_encrypted_response_alg in the test configuration.");
		}

		clientMetaData.addProperty("authorization_encrypted_response_alg", alg);
		clientMetaData.addProperty("authorization_encrypted_response_enc", enc);

		log("Added encryption key to client_metadata in authorization endpoint request", args("client_metadata", clientMetaData));

		return env;
	}
}
