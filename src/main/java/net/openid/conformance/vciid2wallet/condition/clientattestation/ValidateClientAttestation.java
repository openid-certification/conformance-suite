package net.openid.conformance.vciid2wallet.condition.clientattestation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestation extends AbstractVerifyJwsSignature {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject cnfKey = env.getElementFromObject("client_attestation_object", "claims.cnf").getAsJsonObject();

		String clientAttestationPop = env.getString("client_attestation_pop");

		// validate clientattestationpop with key from cnf

		JsonObject jwks = new JsonObject();
		JsonArray jwksKeys = new JsonArray();
		jwksKeys.add(cnfKey);
		jwks.add("keys", jwksKeys);

		verifyJwsSignature(clientAttestationPop, jwks, "client_attestation_pop", false, "dummyjwks");

		return env;
	}
}
