package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

public class VCIValidateCredentialRequestProof extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String proofType = env.getString("proof_type");
		String audience = OIDFJSON.getString(env.getElementFromObject("credential_issuer_metadata","credential_issuer"));

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

		if ("jwt".equals(proofType)) {
			validateJwtProof(env, proofType, audience, credentialConfigurationId, credentialConfiguration, keyAttestationRequired);
			return env;
		} else if ("attestation".equals(proofType)) {
			validateAttestationProof(env, proofType, audience, credentialConfigurationId, credentialConfiguration, keyAttestationRequired);
			return env;
		} else if ("di_vp".equals(proofType)) {
			throw error("Proof type " + proofType + " is currently not supported by the conformance testsuite");
		}

		throw error("Invalid proof type, only 'jwt', 'attestation' or 'di_vp' are allowed.", args("proof_type", proofType));
	}

	protected void validateAttestationProof(Environment env, String proofType, String audience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {
		String attestationJwt = env.getString("proof_attestation", "value");
		try {
			JWKSet publicKeysJwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(attestationJwt);
			JWSHeader header = signedJWT.getHeader();

			String headerType = header.getType().getType();
			if (!"key-attestation+jwt".equals(headerType)) {
				throw error("attestation proof validation failed: Invalid JWT type (typ)",
					args("attestation", attestationJwt, "expected", "key-attestation+jwt", "actual", headerType));
			}
			log("Found expected proof type: " + proofType, args("header", headerType, "proof_type", proofType));

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				throw error("Attestation proof validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256.",
					args("jwt", attestationJwt, "alg", header.getAlgorithm()));
			}
			log("Found expected algorithm for proof type: " + proofType, args("algorithm", header.getAlgorithm()));

			JWK walletPublicKey = extractPublicJwkFromProofJWTHeader(header, attestationJwt, publicKeysJwks);

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				throw error("JWT proof validation failed: Key found but is not an ECKey for kid: " + header.getKeyID());
			}
			log("Detected EC public key", args("kid", header.getKeyID()));

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				throw error("JWT proof validation failed: Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.", args("curve", ecPublicKey.getCurve().getName()));
			}
			log("Detected EC public key with curve P-256", args("kid", header.getKeyID()));

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				throw error("JWT proof validation failed: JWT signature validation failed");
			}
			log("Detected valid proof JWT for proof type: " + proofType);

			// 5. Validate Claims
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// 6. Validation successful :)
			logSuccess("Successfully validated proof type: " + proofType, args("attestation", attestationJwt, "claims", claimsSet));

		} catch (JOSEException e) {
			throw error("Proof validation failed: JOSE error during validation of proof type: " + proofType, e);
		} catch (Exception e) {
			throw error("Proof validation failed: Unexpected error during validation of proof type: " + proofType, e);
		}
	}

	protected void validateJwtProof(Environment env, String proofType, String audience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {

		String jwt = env.getString("proof_jwt", "value");
		try {
			JWKSet publicKeysJwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(jwt);
			JWSHeader header = signedJWT.getHeader();

			// Basic header checks
			String headerType = header.getType().getType();
			if (!"openid4vci-proof+jwt".equals(headerType)) {
				throw error("JWT proof validation failed: Invalid JWT type (typ)",
					args("jwt", jwt, "expected", "openid4vci-proof+jwt", "actual", headerType));
			}
			log("Found expected proof type: " + proofType, args("header", headerType, "proof_type", proofType));

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				throw error("Proof validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256 for proof type: " + proofType,
					args("jwt", jwt, "alg", header.getAlgorithm()));
			}
			log("Found expected algorithm for proof type: " + proofType, args("algorithm", header.getAlgorithm()));

			JWK walletPublicKey = extractPublicJwkFromProofJWTHeader(header, jwt, publicKeysJwks);

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				throw error("JWT proof validation failed: Key found but is not an ECKey for kid: " + header.getKeyID());
			}
			log("Detected EC public key", args("kid", header.getKeyID()));

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				throw error("JWT proof validation failed: Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.",
					args("curve", ecPublicKey.getCurve().getName()));
			}
			log("Detected EC public key with curve P-256", args("kid", header.getKeyID()));

			validateKeyAttestationIfNecessary(credentialConfigurationId, credentialConfiguration, keyAttestationRequired, header, publicKeysJwks);

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				throw error("JWT proof validation failed: JWT signature validation failed");
			}
			log("Detected valid proof JWT for proof type: " + proofType);

			// 5. Validate Claims (No changes needed in claim checking logic itself)
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// Check Nonce
			String expectedNonce = env.getString("credential_issuer_nonce");
			String nonce = claimsSet.getStringClaim("nonce");
			if (!Objects.equals(expectedNonce, nonce)) {
				throw error("JWT proof validation failed: Nonce (nonce) claim mismatch or missing.", args("actual_nonce", nonce, "expected_nonce", expectedNonce));
			} else {
				log("Detected and invalidated expected nonce", args("nonce", nonce));
				env.removeObject("credential_issuer_nonce");
			}

			// Check Issued At
			Date issueTime = claimsSet.getIssueTime();
			if (issueTime == null || Instant.now().plus(5, ChronoUnit.MINUTES).isBefore(issueTime.toInstant())) {
				throw error("JWT proof validation failed: Invalid or missing issued at time (iat) claim.");
			}
			log("Detected JWT proof was issued within the last 5 minutes", args("iat", issueTime.toInstant()));

			// 6. Validation successful :)
			logSuccess("Successfully validated proof jwt", args("jwt", jwt, "claims", claimsSet));

		} catch (JOSEException e) {
			throw error("Proof validation failed: JOSE error during validation of proof type: " + proofType, e);
		} catch (Exception e) {
			throw error("Proof validation failed: Unexpected error during validation of proof type: " + proofType, e);
		}
	}

	protected void validateKeyAttestationIfNecessary(String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired, JWSHeader header, JWKSet publicKeysJwks) {

		Object keyAttestationFromHeader = header.getCustomParam("key_attestation");
		if (keyAttestationFromHeader != null) {
			log("Found key_attestation header",  args("key_attestation", keyAttestationFromHeader));
		}

		if (keyAttestationRequired == null) {
			if (keyAttestationFromHeader != null) {
				log("Skipping key_attestation validation, as it is not required by credential_configuration_id: "+ credentialConfigurationId,
					args("credential_configuration_id", credentialConfigurationId));
			}
			return;
		}

		// key attestation is required for proof
		if (keyAttestationFromHeader == null) {
			throw error("key attestation is not present in 'jwt' proof header but required by credential_configuration_id: " + credentialConfigurationId,
				args("credential_configuration", credentialConfiguration));
		}

		if (!(keyAttestationFromHeader instanceof String)) {
			throw error("Detected key attestation in 'jwt' proof header must be a string containing the key attestation jwt: " + credentialConfigurationId,
				args("key_attestation", keyAttestationFromHeader, "credential_configuration", credentialConfiguration));
		}

		JsonObject keyAttestationVerifiableObject = new JsonObject();
		keyAttestationVerifiableObject.addProperty("verifiable_jws", (String)keyAttestationFromHeader);
		keyAttestationVerifiableObject.addProperty("public_jwk", publicKeysJwks.getKeys().get(0).toString());

		log("Detected key attestation in 'jwt' proof header",
			args("key_attestation_jwt", keyAttestationVerifiableObject));

		// TODO validate key attestation
	}

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
