package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

public class ValidateClientAttestationKeyBindingSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = "client_attestation_object")
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

		JsonObject jwks = JWKUtil.createJwksObjectFromJwkObjects(cnfJwk);

		verifyJwsSignature(clientAttestationPop, jwks, "client_attestation_pop", false, "jwk_from_cnf");

		return env;
	}
}
