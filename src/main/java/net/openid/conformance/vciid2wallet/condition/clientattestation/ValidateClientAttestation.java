package net.openid.conformance.vciid2wallet.condition.clientattestation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestation extends AbstractVerifyJwsSignature {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement cnfEl = env.getElementFromObject("client_attestation_object", "claims.cnf");

		if (cnfEl == null) {
			throw error("Couldn't find cnf claim in the client_attestation");
		}

		JsonObject cnf = cnfEl.getAsJsonObject();
		if (!cnf.has("jwk")) {
			throw error("Couldn't find jwk object in cnf claim of the client_attestation", args("cnf", cnf));
		}

		JsonObject cnfJwk = cnf.getAsJsonObject("jwk");
		log("Found jwk in cnf claim of the client attestation", args("jwk", cnfJwk));

		String clientAttestationPop = env.getString("client_attestation_pop");

		// validate clientattestationpop with key from cnf

		JsonObject jwks = new JsonObject();
		JsonArray jwksKeys = new JsonArray();
		jwksKeys.add(cnfJwk);
		jwks.add("keys", jwksKeys);

		verifyJwsSignature(clientAttestationPop, jwks, "client_attestation_pop", false, "jwk_from_cnf");

		return env;
	}
}
