package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateSdJwtKeyBindingSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "sdjwt" })
	public Environment evaluate(Environment env) {

		String sdjwt = env.getString("sdjwt", "binding.value");
		JsonElement jwk = env.getElementFromObject("sdjwt", "credential.claims.cnf.jwk");
		if (jwk == null) {
			throw error("cnf claim in SD-JWT does not include a jwk element");
		}
		JsonArray jwkArray = new JsonArray();
		jwkArray.add(jwk);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", jwkArray);

		verifyJwsSignature(sdjwt, jwks, "SD-JWT key binding jwt", false, "cnf.jwk from SD-JWT");

		return env;
	}

}
