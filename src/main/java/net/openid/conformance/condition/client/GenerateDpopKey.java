package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class GenerateDpopKey extends AbstractGenerateClientJWKs {

	@Override
	@PreEnvironment(required = {"client", "server"})
	public Environment evaluate(Environment env) {
		// We should allow some configuration of the algorithm used, or at least respect the server's metadata
		// (dpop_signing_alg_values_supported) on what it supports.
		JWKGenerator<? extends JWK> generator;

		String dpopSigningAlg = env.getString("client", "dpop_signing_alg");
		if(Strings.isNullOrEmpty(dpopSigningAlg)) {
			dpopSigningAlg = "PS256";
		}

		JsonElement dpopSupportedAlgs = env.getElementFromObject("server", "dpop_signing_alg_values_supported");
		if(null != dpopSupportedAlgs) {
			if(!((JsonArray)dpopSupportedAlgs).contains(new JsonPrimitive(dpopSigningAlg))) {
				dpopSigningAlg = OIDFJSON.getString(((JsonArray) dpopSupportedAlgs).get(0)); // use first alg in dpop_signing_alg_values_supported if preference is not supported
			}
		}

		switch ( dpopSigningAlg ) {
			case "ES256" :
				generator = new ECKeyGenerator(Curve.P_256).algorithm(JWSAlgorithm.ES256);
				break;
			case "EdDSA":
				generator =  new OctetKeyPairGenerator(Curve.Ed25519).algorithm(JWSAlgorithm.EdDSA);
				break;
			case "PS256":
				generator = new RSAKeyGenerator(DEFAULT_KEY_SIZE).algorithm(JWSAlgorithm.PS256);
				break;
			default:
				throw error("Failed to generate key for alg", args("alg", dpopSigningAlg));
		}

		JWK key;
		try {
			key = generator.keyUse(KeyUse.SIGNATURE).generate();
		} catch (JOSEException e) {
			throw error("Failed to generate DPoP key", e);
		}
		JsonObject keyJson = JsonParser.parseString(key.toJSONString()).getAsJsonObject();
		env.putObject("client", "dpop_private_jwk", keyJson);

		logSuccess("Generated dPOP JWKs", args("private_jwk", keyJson));

		return env;
	}

}
