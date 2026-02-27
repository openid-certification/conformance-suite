package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;

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

		env.putObject("proof_type_object", proofTypeObject);

		String expectedAudience = OIDFJSON.getString(env.getElementFromObject("credential_issuer_metadata","credential_issuer"));
		validateProof(env, proofType, expectedAudience, credentialConfigurationId, credentialConfiguration, keyAttestationRequired);
		return env;
	}

	protected abstract void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired);

	protected JWK extractPublicJwkFromProofJWTHeader(Environment env, JWSHeader header, String jwt) {

		// Check mutual exclusivity of jwk, kid, and x5c
		// See https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-F
		int keyHeaderCount = 0;
		if (header.getJWK() != null) {
			keyHeaderCount++;
		}
		if (header.getKeyID() != null && !header.getKeyID().isEmpty()) {
			keyHeaderCount++;
		}
		if (header.getX509CertChain() != null && !header.getX509CertChain().isEmpty()) {
			keyHeaderCount++;
		}
		if (keyHeaderCount > 1) {
			String errorDescription = "JWT proof header contains more than one of jwk, kid, and x5c; these are mutually exclusive";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("jwt", jwt));
		}

		// 1. try to detect key via jwk header
		// see: check jwk, see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2.1.1
		if (header.getJWK() != null) {
			JWK jwk = header.getJWK().toPublicJWK();
			log("Found public key in jwk header", args("jwk", jwk));
			return jwk;
		}

		// 2. try to detect key via x5c header
		List<Base64> x5c = header.getX509CertChain();
		if (x5c != null && !x5c.isEmpty()) {
			String encodedCert = x5c.get(0).toString();
			byte[] der = java.util.Base64.getDecoder().decode(encodedCert);
			X509Certificate cert = X509CertUtils.parse(der);
			if (cert == null) {
				String errorDescription = "JWT proof validation failed: Could not parse X.509 certificate from x5c header";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("jwt", jwt));
			}

			PublicKey pubKey = cert.getPublicKey();
			JWK key;
			try {
				if (pubKey instanceof ECPublicKey) {
					key = ECKey.parse(cert);
				} else if (pubKey instanceof RSAPublicKey) {
					key = RSAKey.parse(cert);
				} else {
					String errorDescription = "JWT proof validation failed: Unsupported key type in x5c certificate";
					VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
					throw error(errorDescription, args("jwt", jwt));
				}
			} catch (JOSEException e) {
				String errorDescription = "JWT proof validation failed: Could not extract key from x5c certificate";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, e, args("jwt", jwt));
			}
			log("Found public key in x5c header", args("jwk", key.toPublicJWK()));
			return key.toPublicJWK();
		}

		// 3. Find the Wallet's Public Key using 'kid'
		if (header.getKeyID() == null || header.getKeyID().isEmpty()) {
			String errorDescription = "JWT proof validation failed: None of jwk, kid, or x5c found in header";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("jwt", jwt));
		}
		JWKSet publicKeysJwks = null;
		try {
			// client_jwks is potentially mapped to client_jwks2
			publicKeysJwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
		} catch (ParseException e) {
			throw error("JWT proof validation failed: Could not find client jwks with JWK " + header.getKeyID(), args("jwt", jwt, "publicKeysJwks", publicKeysJwks));
		}
		JWK walletPublicKey = publicKeysJwks.getKeyByKeyId(header.getKeyID());
		if (walletPublicKey == null) {
			throw error("JWT proof validation failed: Public key not found for kid: " + header.getKeyID(), args("jwt", jwt, "publicKeysJwks", publicKeysJwks));
		}

		log("Found public key by kid", args("kid", header.getKeyID()));
		return walletPublicKey;
	}
}
