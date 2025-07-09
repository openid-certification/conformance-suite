package net.openid.conformance.vciid2issuer.condition.clientattestation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class CreateClientAttestationJwt extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = {"vci", "config", "client"})
	public Environment evaluate(Environment env) {

		String issuer = env.getString("config", "vci.client_attestation_issuer");
		if (issuer == null || issuer.isBlank()) {
			throw error("Client attestation issuer must not be null or empty");
		}

		String clientId = env.getString("client", "client_id");
		if (clientId == null || clientId.isBlank()) {
			throw error("Client ID must not be null or empty");
		}

		JsonElement clientAttesterKeysJwksEl = env.getElementFromObject("config", "vci.client_attester_keys_jwks");
		if (clientAttesterKeysJwksEl == null) {
			throw error("client_attester_keys_jwks could not be found");
		}

		String clientInstanceKeyPublicString = env.getString("vci", "client_instance_key_public");
		if (clientInstanceKeyPublicString == null) {
			throw error("client_instance_key_public could not be found");
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", clientId);
		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("nbf", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		JsonObject clientInstanceKeyPublic = JsonParser.parseString(clientInstanceKeyPublicString).getAsJsonObject();
		JsonObject cnf = new JsonObject();
		cnf.add("jwk", clientInstanceKeyPublic);
		claims.add("cnf", cnf);

		JsonObject jwks = clientAttesterKeysJwksEl.getAsJsonObject();

		signJWT(env, claims, jwks, true, false,
			true, // see: https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0-03.html#section-4.3.1-2
			true);

		return env;
	}

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("oauth-client-attestation+jwt");
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("client_attestation", jws);
		logSuccess("Generated the Client Attestation JWT", args("client_attestation", verifiableObj));
	}
}
