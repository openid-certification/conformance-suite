package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.time.Instant;
import java.util.UUID;

public class CreateClientAttestationProofJwt extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = {"vci", "config", "client"})
	public Environment evaluate(Environment env) {

		String issuer = env.getString("vci","credential_issuer");
		if (issuer == null || issuer.isBlank()) {
			throw error("Client attestation issuer must not be null or empty");
		}

		String clientId = env.getString("client","client_id");
		if (clientId == null || clientId.isBlank()) {
			throw error("Client ID must not be null or empty");
		}

		String clientInstanceKey = env.getString("vci", "client_instance_key");
		if (clientInstanceKey == null) {
			throw error("clientInstanceKey could not be found");
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", clientId);
		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("nbf", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());
		claims.addProperty("aud", issuer);
		claims.addProperty("jti", UUID.randomUUID().toString());
		// TODO add support for nonce retrieval https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-05#section-8
		// claims.addProperty("nonce", nonce);

		JsonObject jwks = JWKUtil.createJwksObjectFromJwkObjects(JsonParser.parseString(clientInstanceKey).getAsJsonObject());

		signJWT(env, claims, jwks, true);

		return env;
	}

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("oauth-client-attestation-pop+jwt");
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("client_attestation_pop", jws);
		logSuccess("Generated the Client Attestation Proof JWT", args("client_attestation_pop", verifiableObj));
	}
}
