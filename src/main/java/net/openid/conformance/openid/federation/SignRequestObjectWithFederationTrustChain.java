package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SignRequestObjectWithFederationTrustChain extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "request_object_claims", "client_jwks" })
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("request_object_claims").deepCopy();
		JsonObject jwks = env.getObject("client_jwks");

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		try {
			JWK signingJwk = getSigningKey("signing", jwks);
			Algorithm algorithm = signingJwk.getAlgorithm();
			if (algorithm == null) {
				throw error("No 'alg' field specified in key; please add 'alg' field in the configuration", args("jwk", signingJwk));
			}
			JWSAlgorithm alg = JWSAlgorithm.parse(algorithm.getName());

			JWSSignerFactory jwsSignerFactory = MultiJWSSignerFactory.getInstance();
			JWSSigner signer = jwsSignerFactory.createJWSSigner(signingJwk, alg);

			JWSHeader.Builder builder = new JWSHeader.Builder(alg);
			builder.type(getMediaType());
			builder.keyID(signingJwk.getKeyID());

			if (claims.has("trust_chain")) {
				JsonElement trustChain = claims.get("trust_chain");
				if (trustChain.isJsonArray()) {
					List<String> trustChainList = new ArrayList<>();
					for (JsonElement element : trustChain.getAsJsonArray()) {
						trustChainList.add(OIDFJSON.getString(element));
					}
					builder.customParam("trust_chain", trustChainList);
					claims.remove("trust_chain");
				}
			}

			JWSHeader header = builder.build();

			String jws = performSigning(header, claims, signer);

			env.putString("request_object", jws);

			String publicKeySetString = (signingJwk.toPublicJWK() != null ? signingJwk.toPublicJWK().toString() : null);
			JsonObject verifiableObj = new JsonObject();
			verifiableObj.addProperty("verifiable_jws", jws);
			verifiableObj.addProperty("public_jwk", publicKeySetString);

			logSuccess("Signed the request object with trust_chain in header", args("request_object", verifiableObj,
				"header", header,
				"claims", JWTClaimsSet.parse(claims.toString()),
				"key", signingJwk));

			return env;

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error("Unable to sign request object; check provided key has correct 'kty' for it's 'alg': " + e.getCause(), e);
		}
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		// Not used as we override evaluate
	}

}
