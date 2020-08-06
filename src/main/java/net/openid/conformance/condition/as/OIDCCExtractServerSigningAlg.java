package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.dynregistration.AbstractClientValidationCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.List;

public class OIDCCExtractServerSigningAlg extends AbstractClientValidationCondition {

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
		this.client = env.getObject("client");
		String configuredAlg = env.getString("client", "id_token_signed_response_alg");
		if(configuredAlg==null) {
			return selectFirstSigningKeyFromServerJwks(env);
		} else {
			if("none".equals(configuredAlg)) {
				if(hasImplicitResponseTypes()) {
					throw error("none algorithm can only be used when only 'code' response type will be used");
				}
				env.putString("signing_algorithm", "none");
				logSuccess("Using client id_token_signed_response_alg, which is 'none', as the signing algorithm",
					args("signing_algorithm", configuredAlg));
				return env;
			}
			JWSAlgorithm configuredJwsAlgorithm = JWSAlgorithm.parse(configuredAlg);
			if (configuredAlg.startsWith("HS")) {
				if(!JWSAlgorithm.Family.HMAC_SHA.contains(configuredJwsAlgorithm)) {
					throw error("Unexpected algorithm", args("alg", configuredAlg));
				}
				env.putString("signing_algorithm", configuredAlg);
				logSuccess("Using client id_token_signed_response_alg as the signing algorithm",
							args("signing_algorithm", configuredAlg));
				return env;
			} else {
				JsonObject jwks = env.getObject("server_jwks");
				return selectKeyFromServerJwksForAlgorithm(env, jwks, configuredJwsAlgorithm);
			}
		}
	}

	private Environment selectKeyFromServerJwksForAlgorithm(Environment env, JsonObject jwks, JWSAlgorithm configuredJwsAlgorithm) {
		try {
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			KeyType keyType = null;
			if(JWSAlgorithm.Family.RSA.contains(configuredJwsAlgorithm)) {
				keyType = KeyType.RSA;
			} else if(JWSAlgorithm.Family.EC.contains(configuredJwsAlgorithm)) {
				keyType = KeyType.EC;
			} else if(JWSAlgorithm.Family.ED.contains(configuredJwsAlgorithm)) {
				keyType = KeyType.OKP;
			}
			JWSAlgorithm foundAlg = null;

			if (jwkSet != null) {
				List<JWK> keys = jwkSet.getKeys();
				for (JWK key : keys) {
					if (key.getKeyType().equals(keyType)) {
						if(key.getKeyUse() == null || KeyUse.SIGNATURE.equals(key.getKeyUse())) {
							if (key.getAlgorithm() == null) {
								//there may be a more specific match later so don't break
								foundAlg = configuredJwsAlgorithm;
							} else if(key.getAlgorithm().equals(configuredJwsAlgorithm)) {
								//best match, this is it
								foundAlg = configuredJwsAlgorithm;
								break;
							}
						}
					}
				}
			}
			if (foundAlg==null) {
				throw error("Could not find a suitable key in server_jwks for client id_token_signed_response_alg.",
					args("server_jwks", jwks, "id_token_signed_response_alg", configuredJwsAlgorithm.getName()));
			}
			env.putString("signing_algorithm", foundAlg.getName());
			logSuccess( "Selected signing algorithm based on client id_token_signed_response_alg.",
				args("selected_algorithm", foundAlg.getName(), "id_token_signed_response_alg", configuredJwsAlgorithm.getName()));
			return env;
		} catch (ParseException e) {
			throw error("Could not parse server jwks.", e, args("server_jwks", jwks));
		}
	}

	/**
	 * these are the default algorithms based on key type
	 * @param keyType
	 * @return
	 */
	protected JWSAlgorithm getDefaultAlgForKeyType(KeyType keyType) {
		if (KeyType.RSA.equals(keyType)) {
			return JWSAlgorithm.RS256;
		} else if (KeyType.EC.equals(keyType)) {
			return JWSAlgorithm.ES256;
		} else if (KeyType.OKP.equals(keyType)) {
			return JWSAlgorithm.EdDSA;
		} else if (KeyType.OCT.equals(keyType)) {
			return JWSAlgorithm.HS256;
		} else {
			throw error("Unexpected key type", args("key_type", keyType));
		}
	}

	/**
	 * use the alg from the first signing key in server jwks
	 * if the first key has no alg then use RS256, ES256 or EdDSA based on key type
	 * @param env
	 * @return
	 */
	private Environment selectFirstSigningKeyFromServerJwks(Environment env) {
		JsonObject jwks = env.getObject("server_jwks");
		try {
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			for (JWK jwk : jwkSet.getKeys()) {
				if (jwk.getKeyUse() != null && !KeyUse.SIGNATURE.equals(jwk.getKeyUse())) {
					continue;
				}
				Algorithm alg = jwk.getAlgorithm();
				if (alg == null) {
					alg = getDefaultAlgForKeyType(jwk.getKeyType());
					env.putString("signing_algorithm", alg.toString());
					logSuccess("Using the default algorithm for the first key in server jwks",
						args("signing_algorithm", alg.toString()));
					return env;
				} else {
					env.putString("signing_algorithm", alg.toString());
					logSuccess("Using the algorithm for the first key in server jwks",
						args("signing_algorithm", alg.toString()));
					return env;
				}
			}
			throw error("Failed to find a suitable signing key in server jwks", args("server_jwks", jwks));
		} catch (ParseException e) {
			throw error("Failed to parse server_jwks", e, args("server_jwks", jwks));
		}
	}

}
