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
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class VCIValidateCredentialRequestJwtProof extends VCIValidateCredentialRequestAttestationProof {

	@Override
	protected void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {

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

			validateNestedKeyAttestationInJwtProofIfNecessary(env, credentialConfigurationId, credentialConfiguration, keyAttestationRequired, header, publicKeysJwks);

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

			// Check audience
			List<String> audience = claimsSet.getAudience();
			if (audience == null || audience.isEmpty()) {
				throw error("JWT proof validation failed: Missing audience claim",
					args("expected_audience", expectedAudience, "actual_audience", audience));
			}
			if (!List.of(expectedAudience).equals(audience)) {
				throw error("JWT proof validation failed: Expected audience claim to contain " + expectedAudience,
					args("expected_audience", expectedAudience, "actual_audience", audience));
			}
			log("Found expected audience '" + expectedAudience + "' for proof type: " + proofType,
				args("audience", expectedAudience, "proof_type", proofType));

			// 6. Validation successful :)
			logSuccess("Successfully validated proof jwt", args("jwt", jwt, "claims", claimsSet));

		} catch (JOSEException e) {
			throw error("Proof validation failed: JOSE error during validation of proof type: " + proofType, e);
		} catch (Exception e) {
			throw error("Proof validation failed: Unexpected error during validation of proof type: " + proofType, e);
		}
	}

	protected void validateNestedKeyAttestationInJwtProofIfNecessary(Environment env, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired, JWSHeader proofHeader, JWKSet proofPublicKeysJwks) throws Exception {

		Object keyAttestationFromHeader = proofHeader.getCustomParam("key_attestation");
		if (keyAttestationFromHeader != null) {
			log("Found key_attestation header",  args("key_attestation", keyAttestationFromHeader));
		}

		if (keyAttestationRequired == null) {
			if (keyAttestationFromHeader != null) {
				log("Skipping nested key_attestation validation, as it is not required by credential_configuration_id: "+ credentialConfigurationId,
					args("credential_configuration_id", credentialConfigurationId));
			}
			return;
		}

		log("Performing nested key attestation validation");

		// key attestation is required for proof
		if (keyAttestationFromHeader == null) {
			throw error("key attestation is not present in 'jwt' proof header but required by credential_configuration_id: " + credentialConfigurationId,
				args("credential_configuration", credentialConfiguration));
		}

		if (!(keyAttestationFromHeader instanceof String keyAttestationJwt)) {
			throw error("Detected key attestation in 'jwt' proof header must be a string containing the key attestation jwt: " + credentialConfigurationId,
				args("key_attestation", keyAttestationFromHeader, "credential_configuration", credentialConfiguration));
		}

		validateKeyAttestation(env, "jwt", keyAttestationJwt);

		log("Completed nested key attestation validation");
	}

}
