package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.time.Instant;
import java.util.List;

public class VCIGenerateKeyAttestationJwt extends AbstractSignJWT {

	@Override
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id");
		if (clientId == null || clientId.isBlank()) {
			throw error("Client ID must not be null or empty");
		}

		// TODO what Key material to use here? // probably new config setting?
		String clientInstanceKey = env.getString("vci", "client_instance_key");
		if (clientInstanceKey == null) {
			throw error("clientInstanceKey could not be found");
		}

		// D.1. Key Attestation in JWT format See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-D.1
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", clientId); // TODO what is the correct issuer here?
		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		// TODO which key to use here?
		JsonObject attestedKeys = JWKUtil.createJwksObjectFromJwkObjects(JsonParser.parseString(clientInstanceKey).getAsJsonObject());
		claims.add("attested_keys", attestedKeys.getAsJsonArray("keys"));

		// TODO which values to use for key_storage and user_authentication?
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attack-potential-resistance
		claims.add("key_storage", OIDFJSON.convertListToJsonArray(List.of("iso_18045_moderate")));
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attack-potential-resistance
		claims.add("user_authentication", OIDFJSON.convertListToJsonArray(List.of("iso_18045_moderate")));

		// Which JWKS should we use here?
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw error("Credential signing JWK missing from configuration");
		}
		JsonObject jwks = JWKUtil.createJwksObjectFromJwkObjects(credentialSigningJwkEl.getAsJsonObject());

		signJWT(env, claims, jwks, true);

		return env;
	}

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("key-attestation+jwt");
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("key_attestation_jwt", jws);
		logSuccess("Generated the Key Attestation JWT", args("key_attestation_jwt", verifiableObj));
	}
}
