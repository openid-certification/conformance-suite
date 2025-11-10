package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;

public class VCIGenerateKeyAttestationJwtIfNecessary extends AbstractSignJWT {

	@Override
	@PreEnvironment(strings = "vci_proof_type_key", required = {"vci_credential_configuration", "vci_proof_type"})
	public Environment evaluate(Environment env) {

		// determine if requested credential requires key attestation
		String proofTypeKey = env.getString("vci_proof_type_key");
		JsonObject proofType = env.getObject("vci_proof_type");
		if (!proofType.has("key_attestations_required")) {
			log("Skipping Key attestation generation because proof type " + proofTypeKey + " does not contain 'key_attestations_required'",
				args("proof_type", proofType, "proof_type_key", proofTypeKey));
			return env;
		}

		JWKSet jwksForProof;
		try {
			jwksForProof = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		// D.1. Key Attestation in JWT format See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-D.1
		JsonObject claims = new JsonObject();
		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		JsonObject attestedKeys = JWKUtil.getPublicJwksAsJsonObject(jwksForProof);

		// we need to use the keys here that are used for singing the proof
		claims.add("attested_keys", attestedKeys.getAsJsonArray("keys"));

		// TODO are we using the right values for key_storage and user_authentication here?
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attack-potential-resistance
		claims.add("key_storage", OIDFJSON.convertListToJsonArray(List.of("iso_18045_moderate")));
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attack-potential-resistance
		claims.add("user_authentication", OIDFJSON.convertListToJsonArray(List.of("iso_18045_moderate")));

		JsonObject keyAttestationJwks = env.getObject("vci_key_attestation_jwks");
		if (keyAttestationJwks == null) {
			throw error("Required Key Attestation JWKS could not be found");
		}

		signJWT(env, claims, keyAttestationJwks, true, false, false, false);

		return env;
	}

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("key-attestation+jwt");
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("key_attestation_jwt", jws);
		logSuccess("Generated Key Attestation JWT", args("key_attestation_jwt", verifiableObj));
	}
}
