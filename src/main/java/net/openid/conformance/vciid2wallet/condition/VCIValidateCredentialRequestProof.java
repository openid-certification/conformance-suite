package net.openid.conformance.vciid2wallet.condition;

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
import net.openid.conformance.util.JWKUtil;

import java.io.Serial;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class VCIValidateCredentialRequestProof extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String jwt = env.getString("proof_jwt", "value");
		String audience = env.getString("server", "issuer");

		JWTClaimsSet jwtClaimsSet;
		try {
			JWKSet publicKeysJwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			jwtClaimsSet = validateJwtProof(jwt, audience, null, publicKeysJwks);
			logSuccess("Successfully validated proof jwt", args("jwt", jwt, "claims", jwtClaimsSet));
		} catch (Exception e) {
			String message = e.getMessage();
			if (e.getCause() != null) {
				message += " Cause: " + e.getCause().getMessage();
			}
			throw error("JWT proof validation failed", args("jwt", jwt, "error", message));
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
	 * @param proofJwt         The compact JWT proof string received from the Wallet.
	 * @param expectedAudience The Issuer's own identifier (must match 'aud' claim).
	 * @param expectedNonce    The 'c_nonce' the Issuer provided to the Wallet (must match 'nonce' claim).
	 * @param walletPublicKeys A JWKSet containing trusted public keys of Wallets (used to find RSA key via 'kid').
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

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				throw new JwtProofValidationException("Unsupported or invalid JWT algorithm (alg). Expected ES256.");
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

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				throw new InvalidPublicKeyException("Key found but is not an ECKey for kid: " + header.getKeyID());
			}

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				throw new InvalidPublicKeyException("Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.");
			}

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

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
