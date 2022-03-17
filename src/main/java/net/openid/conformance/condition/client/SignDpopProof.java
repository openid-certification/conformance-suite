package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Header;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.function.Consumer;

public class SignDpopProof extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dpop_proof_claims", "client" })
	@PostEnvironment(strings = "dpop_proof")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("dpop_proof_claims");
		JsonObject jwk = (JsonObject) env.getElementFromObject("client", "dpop_private_jwk");
		if (jwk == null) {
			throw error("No dpop_private_jwk found.");
		}

		try {
			JWSAlgorithm alg = JWSAlgorithm.parse("PS256");
			if (alg == null) {
				throw error("No 'alg' field");
			}

			JWK signingJwk = JWK.parse(jwk.toString());
			JWSHeader.Builder headerBuilder = new JWSHeader.Builder(alg);
			headerBuilder.type(new JOSEObjectType("dpop+jwt"));
			headerBuilder.jwk(signingJwk.toPublicJWK());
			JWSHeader header = headerBuilder.build();

			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());

			JWSSignerFactory jwsSignerFactory = new DefaultJWSSignerFactory();
			JWSSigner signer = jwsSignerFactory.createJWSSigner(signingJwk, alg);

			SignedJWT signJWT = new SignedJWT(header, claimSet);

			signJWT.sign(signer);

			String jws = signJWT.serialize();

			String publicKeySetString = signingJwk.toPublicJWK().toString();
			JsonObject verifiableObj = new JsonObject();
			verifiableObj.addProperty("verifiable_jws", jws);
			verifiableObj.addProperty("public_jwk", publicKeySetString);

			env.putString("dpop_proof", jws);

			logSuccess("Signed the DPoP proof", args("dpop_proof", verifiableObj, "key", signingJwk));

			return env;

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error("Unable to sign dpop proof: " + e.getCause(), e);
		}
	}
}
