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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;
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
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(jwt);
			JWSHeader header = signedJWT.getHeader();

			// Basic header checks
			String headerType = header.getType().getType();
			if (!"openid4vci-proof+jwt".equals(headerType)) {
				String errorDescription = "JWT proof validation failed: Invalid JWT type (typ)";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("jwt", jwt, "expected", "openid4vci-proof+jwt", "actual", headerType));
			}
			log("Found expected proof type: " + proofType, args("header", headerType, "proof_type", proofType));

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				String errorDescription = "Proof validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256 for proof type: " + proofType;
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("jwt", jwt, "alg", header.getAlgorithm()));
			}
			log("Found expected algorithm for proof type: " + proofType, args("algorithm", header.getAlgorithm()));

			JWK walletPublicKey = extractPublicJwkFromProofJWTHeader(env, header, jwt);
			env.putObject("proof_jwt", "jwk", OIDFJSON.convertMapToJsonObject(walletPublicKey.toJSONObject()));

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				String errorDescription = "JWT proof validation failed: Key found but is not an ECKey for kid: " + header.getKeyID();
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription);
			}
			log("Detected an EC public key", args("kid", header.getKeyID()));

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				String errorDescription = "JWT proof validation failed: Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("curve", ecPublicKey.getCurve().getName()));
			}
			log("Detected EC public key with curve P-256", args("kid", header.getKeyID()));

			validateNestedKeyAttestationInJwtProofIfNecessary(env, credentialConfigurationId, credentialConfiguration, keyAttestationRequired, header);

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				String errorDescription = "JWT proof validation failed: JWT signature validation failed";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription);
			}
			log("Detected valid proof JWT for proof type: " + proofType);

			// 5. Validate Claims (No changes needed in claim checking logic itself)
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// Check Nonce
			String expectedNonce = env.getString("credential_issuer_nonce");
			String nonce = claimsSet.getStringClaim("nonce");
			if (!Objects.equals(expectedNonce, nonce)) {
				String errorDescription = "JWT proof validation failed: Nonce (nonce) claim mismatch or missing.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_NONCE, errorDescription);
				throw error(errorDescription, args("actual_nonce", nonce, "expected_nonce", expectedNonce));
			} else {
				log("Detected and invalidated expected nonce", args("nonce", nonce));
				env.removeNativeValue("credential_issuer_nonce");
			}

			// Check Issued At
			Date issueTime = claimsSet.getIssueTime();
			if (issueTime == null) {
				String errorDescription = "JWT proof validation failed: Missing issued at time (iat) claim.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription);
			}
			Instant iatInstant = issueTime.toInstant();
			Instant now = Instant.now();
			Instant maxAcceptedIat = now.plus(5, ChronoUnit.MINUTES);
			if (iatInstant.isAfter(maxAcceptedIat)) {
				String errorDescription = "JWT proof validation failed: Issued at time (iat) is too far in the future.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("iat", iatInstant, "now", now, "max_accepted_iat", maxAcceptedIat));
			}
			Instant minAcceptedIat = now.minus(5, ChronoUnit.MINUTES);
			if (iatInstant.isBefore(minAcceptedIat)) {
				String errorDescription = "JWT proof validation failed: Issued at time (iat) is too far in the past.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("iat", iatInstant, "now", now, "min_accepted_iat", minAcceptedIat));
			}
			log("JWT proof iat is within acceptable time window", args("iat", iatInstant, "now", now, "min_accepted_iat", minAcceptedIat, "max_accepted_iat", maxAcceptedIat));

			// Check audience
			List<String> audience = claimsSet.getAudience();
			if (audience == null || audience.isEmpty()) {
				String errorDescription = "JWT proof validation failed: Missing audience claim";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("expected_audience", expectedAudience, "actual_audience", audience));
			}
			if (!List.of(expectedAudience).equals(audience)) {
				String errorDescription = "JWT proof validation failed: Expected audience claim to contain " + expectedAudience;
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("expected_audience", expectedAudience, "actual_audience", audience));
			}
			log("Found expected audience '" + expectedAudience + "' for proof type: " + proofType,
				args("audience", expectedAudience, "proof_type", proofType));

			// 6. Validation successful :)
			logSuccess("Successfully validated proof jwt", args("jwt", jwt, "claims", claimsSet));

		} catch (JOSEException e) {
			String errorDescription = "Proof validation failed: JOSE error during validation of proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e);
		}
		catch (ParseException e) {
			String errorDescription = "Proof validation failed: Unexpected error during validation of proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e);
		}
	}

	protected void validateNestedKeyAttestationInJwtProofIfNecessary(Environment env, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired, JWSHeader proofHeader) {

		Object keyAttestationFromHeader = proofHeader.getCustomParam("key_attestation");
		if (keyAttestationFromHeader != null) {
			log("Found key_attestation header",  args("key_attestation", keyAttestationFromHeader));
		}

		if (keyAttestationRequired == null) {
			log("Skipping nested key_attestation validation, as it is not required by credential_configuration_id: "+ credentialConfigurationId,
				args("credential_configuration_id", credentialConfigurationId, "key_attestation", keyAttestationFromHeader));
			return;
		}

		log("Performing nested key attestation validation");

		// key attestation is required for proof
		if (keyAttestationFromHeader == null) {
			String errorDescription = "key attestation is not present in 'jwt' proof header but required by credential_configuration_id: " + credentialConfigurationId;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("credential_configuration", credentialConfiguration));
		}

		if (!(keyAttestationFromHeader instanceof String keyAttestationJwt)) {
			String errorDescription = "Detected key attestation in 'jwt' proof header must be a string containing the key attestation jwt: " + credentialConfigurationId;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription,
				args("key_attestation", keyAttestationFromHeader, "credential_configuration", credentialConfiguration));
		}

		validateKeyAttestation(env, "jwt", keyAttestationJwt);

		log("Completed nested key attestation validation");
	}

}
