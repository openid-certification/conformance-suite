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
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;

public class VCIGenerateKeyAttestationIfNecessary extends AbstractSignJWT {

	@Override
	@PreEnvironment(strings = "vci_proof_type_key", required = {"vci_credential_configuration", "vci_proof_type"})
	public Environment evaluate(Environment env) {

		// determine if requested credential requires key attestation
		String proofTypeKey = env.getString("vci_proof_type_key");
		JsonObject proofType = env.getObject("vci_proof_type");

		if (!(proofType.has("key_attestations_required") || "attestation".equals(proofTypeKey))) {
			log("Skipping Key attestation generation because proof type " + proofTypeKey + " does not contain 'key_attestations_required' or proof key type is not 'attestation'",
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

		// The issuer advertises, under key_attestations_required, the attack-potential-resistance
		// values it accepts for each component. As a cooperative wallet, assert exactly those so the
		// attestation satisfies the issuer regardless of how it matches them. Both sub-parameters are
		// OPTIONAL, so only assert a component the issuer actually requires; when none are required
		// the claims are omitted (also OPTIONAL in the attestation).
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attack-potential-resistance
		if (proofType.has("key_attestations_required") && proofType.get("key_attestations_required").isJsonObject()) {
			JsonObject keyAttestationsRequired = proofType.getAsJsonObject("key_attestations_required");
			copyRequiredComponent(keyAttestationsRequired, claims, "key_storage");
			copyRequiredComponent(keyAttestationsRequired, claims, "user_authentication");
		}

		// if we received a nonce value, then add it to the key attestation
		String cNonce = env.getString("vci", "c_nonce");
		if (cNonce != null && !cNonce.isBlank()) {
			claims.addProperty("nonce", cNonce);
		}

		// TODO add status claim if necessary

		JsonObject keyAttestationJwks = env.getObject("vci_key_attestation_jwks");
		if (keyAttestationJwks == null) {
			throw error("Required Key Attestation JWKS could not be found");
		}

		signJWT(env, claims, keyAttestationJwks, true, false, true, true);

		return env;
	}

	/**
	 * Copies the issuer's required attack-potential-resistance values for a key attestation component
	 * (key_storage / user_authentication) from key_attestations_required into the attestation claims,
	 * if the issuer requires that component. Each component is OPTIONAL, so an absent one is left out.
	 */
	protected void copyRequiredComponent(JsonObject keyAttestationsRequired, JsonObject claims, String component) {
		if (keyAttestationsRequired.has(component) && keyAttestationsRequired.get(component).isJsonArray()) {
			claims.add(component, keyAttestationsRequired.getAsJsonArray(component).deepCopy());
		}
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
