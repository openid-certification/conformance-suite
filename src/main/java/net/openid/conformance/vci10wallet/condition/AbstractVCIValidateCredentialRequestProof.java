package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractVCIValidateCredentialRequestProof extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String proofType = env.getString("proof_type");

		String credentialConfigurationId  = env.getString("credential_configuration_id");
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		JsonObject proofTypesSupported = credentialConfiguration.getAsJsonObject("proof_types_supported");

		if (!proofTypesSupported.has(proofType)) {
			throw error("Given proof type '" + proofType + "' for credential is not allowed by credential_configuration_id: " + credentialConfigurationId,
				args("proof_type", proofType, "credential_configuration_id", credentialConfigurationId));
		}
		log("Detected proof type " + proofType + " is allowed by credential_configuration_id: " + credentialConfigurationId,
			args("proof_type", proofType, "credential_configuration_id", credentialConfigurationId));

		JsonObject proofTypeObject = proofTypesSupported.getAsJsonObject(proofType);
		JsonObject keyAttestationRequired = proofTypeObject.getAsJsonObject("key_attestations_required");

		String expectedAudience = OIDFJSON.getString(env.getElementFromObject("credential_issuer_metadata","credential_issuer"));
		validateProof(env, proofType, expectedAudience, credentialConfigurationId, credentialConfiguration, keyAttestationRequired);
		return env;
	}

	protected abstract void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired);

	protected JWK extractPublicJwkFromProofJWTHeader(JWSHeader header, String jwt, JWKSet publicKeysJwks) {

		// 1. try to detect key via jwk header
		// see: check jwk, see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2.1.1
		if (header.getJWK() != null) {
			JWK jwk = header.getJWK().toPublicJWK();
			log("Found public key in jwk header", args("jwk", jwk));
			return jwk;
		}

		// 2. Find the Wallet's Public Key using 'kid'
		if (header.getKeyID() == null || header.getKeyID().isEmpty()) {
			throw error("JWT proof validation failed: Missing Key ID (kid) in header", args("jwt", jwt));
		}

		JWK walletPublicKey = publicKeysJwks.getKeyByKeyId(header.getKeyID());
		if (walletPublicKey == null) {
			throw error("JWT proof validation failed: Public key not found for kid: " + header.getKeyID(), args("jwt", jwt, "publicKeysJwks", publicKeysJwks));
		}

		log("Found public key by kid", args("kid", header.getKeyID()));
		return walletPublicKey;
	}
}
