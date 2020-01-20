package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.List;

public class OIDCCExtractServerSigningAlg extends AbstractCondition {

	/**
	 * if client has id_token_signed_response_alg use that if we have a suitable key
	 * else use the alg for the first key in server_jwks, should default to RS256
	 *
	 * MUST be called after dynamic client registration when using dynamic client registration
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = {"server_jwks", "client"})
	@PostEnvironment(strings = "signing_algorithm")
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("server_jwks");

		String configuredAlg = env.getString("client", "id_token_signed_response_alg");
		if(configuredAlg==null) {
			try {
				//use the alg from the first key in server jwks
				JWKSet jwkSet = JWKSet.parse(jwks.toString());
				JWK jwk = jwkSet.getKeys().iterator().next();
				Algorithm alg = jwk.getAlgorithm();
				if (alg == null) {
					//this is unlikely to happen since we are creating the jwks
					throw error("No algorithm specified for the first key in server_jwks", args("jwk", jwk.toJSONString()));
				} else {
					env.putString("signing_algorithm", alg.toString());
					logSuccess("Using the algorithm for the first key in server jwks", args("signing_algorithm", alg.toString()));
					return env;
				}
			} catch (ParseException e) {
				throw error("Failed to parse server_jwks", e, args("server_jwks", jwks));
			}
		} else {

			JWK selectedKey = null;
			if (configuredAlg.startsWith("HS")) {
				//using MAC based alg
				env.putString("signing_algorithm", configuredAlg);
				logSuccess("Using client id_token_signed_response_alg as the signing algorithm", args("signing_algorithm", configuredAlg));
				return env;
			} else {
				try {
					JWKSet jwkSet = JWKSet.parse(jwks.toString());
					String foundAlg = null;
					KeyType keyType = (configuredAlg.startsWith("E")?KeyType.EC:KeyType.RSA);
					if (jwkSet != null) {
						List<JWK> keys = jwkSet.getKeys();
						for (JWK key : keys) {
							if (key.getKeyUse().equals(KeyUse.SIGNATURE) && key.getKeyType().equals(keyType)
								&& configuredAlg.startsWith(key.getAlgorithm().getName().substring(0,2))
							) {
								foundAlg = key.getAlgorithm().getName();
								break;
							}
						}
					}
					if (foundAlg!=null) {
						env.putString("signing_algorithm", foundAlg);
						String logMsg = "Selected signing algorithm based on client id_token_signed_response_alg.";
						if(!configuredAlg.equals(foundAlg)) {
							logMsg += " Please note that the suite defaults to a xx256 algorithm " +
								"even if id_token_signed_response_alg contains a xx384 or xx512 version.";
						}
						logSuccess( logMsg, args("selected_algorithm", foundAlg, "id_token_signed_response_alg", configuredAlg));
						return env;
					} else {
						throw error("Could not find a suitable key in server_jwks for client id_token_signed_response_alg.",
							args("server_jwks", jwks, "id_token_signed_response_alg", configuredAlg));
					}
				} catch (ParseException e) {
					throw error("Could not parse server jwks.", e, args("server_jwks", jwks));
				}
			}
		}
	}

}
