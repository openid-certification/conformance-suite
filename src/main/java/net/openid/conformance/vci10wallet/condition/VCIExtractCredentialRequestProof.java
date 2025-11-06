package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.io.Serial;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class VCIExtractCredentialRequestProof extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialRequestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();

		boolean proofPresent = credentialRequestBodyJson.has("proof");
		boolean proofsPresent = credentialRequestBodyJson.has("proofs");

		if (!proofPresent && !proofsPresent) {
			throw error("Neither proof nor proofs element is specified in credential request");
		}

		if (proofPresent == proofsPresent) {
			throw error("In credential requests, only 'proof' or 'proofs' elements may be used, but not both.");
		}

		if (proofPresent) {
			// TODO remove this after upgrading to OPENIDVCI-draft16+
			JsonElement proofEl = credentialRequestBodyJson.get("proof");
			log("Found proof element in credential request", args("proof", proofEl));
			JsonObject proofObject = proofEl.getAsJsonObject();
			String proofType = OIDFJSON.getString(proofObject.get("proof_type"));
			log("Detected proof type", args("proof_type", proofType));
			if ("jwt".equals(proofType)) {
				String jwtString = OIDFJSON.getString(proofObject.get("jwt"));
				try {
					JsonObject proofJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
					env.putObject("proof_jwt", proofJwt);
				} catch (ParseException e) {
					throw error("Parsing SD-JWT credential jwt failed", e, args("proof_jwt_string", jwtString));
				}
			} else if ("attestation".equals(proofType)) {

				// TODO this would allow to pass attestaton proof types through

				// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-F-3.3
				JsonArray attestationArray = proofObject.getAsJsonArray("attestation");
				if (attestationArray == null) {
					throw error("Expected attestation array in proof object, but found " + attestationArray.size(), args("proof_type", proofType, "attestation", attestationArray));
				}
				if (attestationArray.size() != 1) {
					throw error("Expected attestation array with a single JWT in proof object, but found " + attestationArray.size(), args("proof_type", proofType, "attestation", attestationArray));
				}
				String jwtString = OIDFJSON.getString(attestationArray.get(0));
				try {
					// D.1. Key Attestation in JWT format
					// See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-D.1
					JsonObject proofJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
					// TODO is this really a proof_jwt, or rather a key_attestation_jwt ?
					env.putObject("proof_jwt", proofJwt);

					// TODO verify this in a separate condition, e.g. VCIVerifyKeyAttestationProof (after this condition)
				} catch (ParseException e) {
					throw error("Parsing SD-JWT credential attestation jwt failed", e, args("proof_jwt_string", jwtString));
				}
			} else {
				throw error("Unsupported proof type found in proof element", args("proof_type", proofType));
			}
		}

		if (proofsPresent) {
			JsonElement proofsEl = credentialRequestBodyJson.get("proofs");
			log("Found proofs element in credential request", args("proofs", proofsEl));
			JsonObject proofsObject = proofsEl.getAsJsonObject();

			if (!proofsObject.has("jwt")) {
				throw error("Missing jwt entry in in proofs element");
			}

			JsonElement jwtEl = proofsObject.get("jwt");
			if (!jwtEl.isJsonArray()) {
				throw error("Type of jwt attribute value in proofs element must be a JSON array", args("jwt", jwtEl));
			}

			JsonArray jwtArray = jwtEl.getAsJsonArray();
			if (jwtArray.isEmpty()) {
				throw error("jwt array must not be empty", args("jwt", jwtEl));
			}

			// for now we select the first item in the array
			String jwtString = OIDFJSON.getString(jwtArray.get(0));
			try {
				JsonObject proofJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
				env.putObject("proof_jwt", proofJwt);
			} catch (ParseException e) {
				throw error("Parsing SD-JWT credential jwt failed", e, args("proof_jwt_string", jwtString));
			}
		}

		return env;
	}

	public static class JwtProofValidationException extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		public JwtProofValidationException(String message) {
			super(message);
		}

		public JwtProofValidationException(String message, JOSEException e) {
			super(message, e);
		}

		public JwtProofValidationException(String message, Exception e) {
			super(message, e);
		}
	}

	public static class InvalidPublicKeyException extends JwtProofValidationException {
		@Serial
		private static final long serialVersionUID = 1L;
		public InvalidPublicKeyException(String message) {
			super(message);
		}
	}
	public static class InvalidClaimException extends JwtProofValidationException {
		@Serial
		private static final long serialVersionUID = 1L;
		public InvalidClaimException(String message) {
			super(message);
		}
	}

	/**
	 * Validates a received OID4VCI JWT proof signed with PS256.
	 *
	 * @param proofJwt           The compact JWT proof string received from the Wallet.
	 * @param expectedAudience   The Issuer's own identifier (must match 'aud' claim).
	 * @param expectedNonce      The 'c_nonce' the Issuer provided to the Wallet (must match 'nonce' claim).
	 * @param walletPublicKeys   A JWKSet containing trusted public keys of Wallets (used to find RSA key via 'kid').
	 * @return The validated JWTClaimsSet if successful.
	 * @throws JwtProofValidationException If validation fails for any reason (signature, claims, etc.).
	 */
	public JWTClaimsSet validateJwtProof(
		String proofJwt,
		String expectedAudience,
		String expectedNonce,
		JWKSet walletPublicKeys) throws JwtProofValidationException {

		try {
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(proofJwt);
			JWSHeader header = signedJWT.getHeader();

			// Basic header checks
			if (!"openid4vci-proof+jwt".equals(header.getType().getType())) {
				throw new JwtProofValidationException("Invalid JWT type (typ)");
			}

			// TODO add support for other algorithms
			// *** Algorithm Check Changed ***
			if (!JWSAlgorithm.PS256.equals(header.getAlgorithm())) {
				throw new JwtProofValidationException("Unsupported or invalid JWT algorithm (alg). Expected PS256.");
			}

			// TODO add support to check jwk, see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2.1.1
			if (header.getKeyID() == null || header.getKeyID().isEmpty()) {
				throw new JwtProofValidationException("Missing Key ID (kid) in header");
			}

			// TODO add support to detect key via jwk header
			// 2. Find the Wallet's Public Key using 'kid'
			JWK walletPublicKey = walletPublicKeys.getKeyByKeyId(header.getKeyID());
			if (walletPublicKey == null) {
				throw new InvalidPublicKeyException("Public key not found for kid: " + header.getKeyID());
			}

			// *** Key Type Check Changed ***
			if (!(walletPublicKey instanceof RSAKey)) {
				throw new InvalidPublicKeyException("Key found but is not an RSAKey for kid: " + header.getKeyID());
			}

			// 3. Create Verifier with the public RSA key
			JWSVerifier verifier = new RSASSAVerifier((RSAKey) walletPublicKey); // Changed to RSASSAVerifier

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				throw new JwtProofValidationException("JWT signature validation failed");
			}

			// 5. Validate Claims (No changes needed in claim checking logic itself)
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// Check Audience
			List<String> audiences = claimsSet.getAudience();
			if (audiences == null || !audiences.contains(expectedAudience)) {
				throw new InvalidClaimException("Invalid or missing audience (aud) claim. Expected: " + expectedAudience);
			}

			// Check Nonce
			String nonce = claimsSet.getStringClaim("nonce");
			if (!Objects.equals(expectedNonce, nonce)) {
				throw new InvalidClaimException("Nonce (nonce) claim mismatch or missing.");
			}

			// Check Expiration
			Date expirationTime = claimsSet.getExpirationTime();
			if (expirationTime == null || Instant.now().isAfter(expirationTime.toInstant())) {
				throw new InvalidClaimException("JWT has expired or expiration time (exp) is missing.");
			}

			// Check Issued At
			Date issueTime = claimsSet.getIssueTime();
			if (issueTime == null || Instant.now().plus(5, ChronoUnit.MINUTES).isBefore(issueTime.toInstant())) {
				throw new InvalidClaimException("Invalid or missing issued at time (iat) claim.");
			}

			// 6. Validation successful :)
			return claimsSet;

		} catch (JOSEException e) {
			throw new JwtProofValidationException("JOSE error during validation", e);
		} catch (Exception e) {
			throw new JwtProofValidationException("Unexpected error during validation", e);
		}
	}
}
