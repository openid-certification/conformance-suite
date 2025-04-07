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

		try {
			JWKSet publicKeysJwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(jwt);
			JWSHeader header = signedJWT.getHeader();

			// TODO determine expected nonce
			String expectedNonce = null;

			// Basic header checks
			if (!"openid4vci-proof+jwt".equals(header.getType().getType())) {
				throw error("JWT proof validation failed: Invalid JWT type (typ)", args("jwt", jwt,
					"expected", "openid4vci-proof+jwt",
					"actual", header.getType().getType()));
			}

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				throw error("JWT proof validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256.", args("jwt", jwt, "alg", header.getAlgorithm()));
			}

			// TODO add support to check jwk, see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2.1.1
			if (header.getKeyID() == null || header.getKeyID().isEmpty()) {
				throw error("JWT proof validation failed: Missing Key ID (kid) in header", args("jwt", jwt));
			}

			// TODO add support to detect key via jwk header
			// 2. Find the Wallet's Public Key using 'kid'
			JWK walletPublicKey = publicKeysJwks.getKeyByKeyId(header.getKeyID());
			if (walletPublicKey == null) {
				throw error("JWT proof validation failed: Public key not found for kid: " + header.getKeyID(), args("jwt", jwt, "publicKeysJwks", publicKeysJwks));
			}

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				throw error("JWT proof validation failed: Key found but is not an ECKey for kid: " + header.getKeyID());
			}

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				throw error("JWT proof validation failed: Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.", args("curve", ecPublicKey.getCurve().getName()));
			}

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				throw error("JWT proof validation failed: JWT signature validation failed");
			}

			// 5. Validate Claims (No changes needed in claim checking logic itself)
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// Check Audience
			List<String> audiences = claimsSet.getAudience();
			if (audiences == null || !audiences.contains(audience)) {
				throw error("JWT proof validation failed: Invalid or missing audience (aud) claim. Expected audience to contain: " + audience, args("aud", audiences));
			}

			// Check Nonce
			String nonce = claimsSet.getStringClaim("nonce");
			if (!Objects.equals(expectedNonce, nonce)) {
				throw error("JWT proof validation failed: Nonce (nonce) claim mismatch or missing.", args("actual_nonce", nonce, "expected_nonce", expectedNonce));
			}

			// Check Expiration
			Date expirationTime = claimsSet.getExpirationTime();
			if (expirationTime == null || Instant.now().isAfter(expirationTime.toInstant())) {
				throw error("JWT proof validation failed: JWT has expired or expiration time (exp) is missing.");
			}

			// Check Issued At
			Date issueTime = claimsSet.getIssueTime();
			if (issueTime == null || Instant.now().plus(5, ChronoUnit.MINUTES).isBefore(issueTime.toInstant())) {
				throw error("JWT proof validation failed: Invalid or missing issued at time (iat) claim.");
			}

			// 6. Validation successful :)
			logSuccess("Successfully validated proof jwt", args("jwt", jwt, "claims", claimsSet));

		} catch (JOSEException e) {
			throw error("JWT proof validation failed: JOSE error during validation", e);
		} catch (Exception e) {
			throw error("JWT proof validation failed: Unexpected error during validation", e);
		}

		return env;
	}
}
