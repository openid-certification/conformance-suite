package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIGenerateClientJwksIfMissing extends AbstractCondition {
	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		generateJwksForClient1(env);
		generateJwksForClient2(env);

		return env;
	}

	protected void generateJwksForClient1(Environment env) {
		JsonElement jwks = env.getElementFromObject("client", "jwks");
		if (jwks != null) {
			log("Client JWKS already present in configuration, skipping generation");
			return;
		}

		JsonObject generated = generateEcJwks();
		env.putObject("client", "jwks", generated);
		logSuccess("Generated JWKS for client", args("jwks", generated));
	}

	protected void generateJwksForClient2(Environment env) {
		JsonElement client2Config = env.getElementFromObject("config", "client2");
		if (client2Config == null) {
			return;
		}

		JsonElement jwks = env.getElementFromObject("config", "client2.jwks");
		if (jwks != null) {
			log("Client2 JWKS already present in configuration, skipping generation");
			return;
		}

		JsonObject generated = generateEcJwks();
		env.putObject("config", "client2.jwks", generated);
		logSuccess("Generated JWKS for client2", args("jwks", generated));
	}

	private JsonObject generateEcJwks() {
		JWK key;
		try {
			key = new ECKeyGenerator(Curve.P_256)
				.algorithm(JWSAlgorithm.ES256)
				.keyUse(KeyUse.SIGNATURE)
				.keyIDFromThumbprint(true)
				.generate();
		} catch (JOSEException e) {
			throw error("Failed to generate EC key", e);
		}

		JWKSet keySet = new JWKSet(key);
		return JsonParser.parseString(keySet.toString(false)).getAsJsonObject();
	}
}
