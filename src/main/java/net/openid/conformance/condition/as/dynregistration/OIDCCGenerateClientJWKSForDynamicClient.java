package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractGenerateClientJWKs;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDCCGenerateClientJWKSForDynamicClient extends AbstractGenerateClientJWKs {
	@PreEnvironment(required = {"client"})
	@PostEnvironment(required = {"client"})
	@Override
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		if(client.has("jwks") || client.has("jwks_uri")) {
			logSuccess("Registration request contains jwks or jwks_uri, not generating new JWKS");
			return env;
		}
		String idTokenSigAlg = null;

		if(client.has("id_token_signed_response_alg")) {
			idTokenSigAlg = OIDFJSON.getString(client.get("id_token_signed_response_alg"));
		}

		if(idTokenSigAlg==null) {
			JWKGenerator<RSAKey> generator = new RSAKeyGenerator(DEFAULT_KEY_SIZE).algorithm(JWSAlgorithm.RS256);
			generateClientJWKs(env, generator);
		} else if (idTokenSigAlg.startsWith("RS")) {
			JWKGenerator<RSAKey> generator = new RSAKeyGenerator(DEFAULT_KEY_SIZE).algorithm(JWSAlgorithm.RS256);
			generateClientJWKs(env, generator);
		} else if(idTokenSigAlg.startsWith("ES")) {
			JWKGenerator<ECKey> generator = new ECKeyGenerator(Curve.P_256).algorithm(JWSAlgorithm.ES256);
			generateClientJWKs(env, generator);
		}
		else if(idTokenSigAlg.startsWith("HS")) {
			JWKGenerator<OctetSequenceKey> generator = new OctetSequenceKeyGenerator(256).algorithm(JWSAlgorithm.HS256);
			generateClientJWKs(env, generator);
		}
		JsonObject generatedClientJwks = env.getObject("client_jwks");
		client.add("jwks", generatedClientJwks);
		logSuccess("Generated new JWKS for the client", args("client", env.getObject("client")));
		return env;
	}
}
