package net.openid.conformance.vci10wallet.condition;

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
import java.util.List;
import java.util.Objects;

public class VCIValidateCredentialRequestProof extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String jwt = env.getString("proof_jwt", "value");
		String audience = OIDFJSON.getString(env.getElementFromObject("credential_issuer_metadata","credential_issuer"));

		try {
			JWKSet publicKeysJwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(jwt);
			JWSHeader header = signedJWT.getHeader();

			String expectedNonce = env.getString("credential_issuer_nonce");

			// Basic header checks
			String headerType = header.getType().getType();
			if (!"openid4vci-proof+jwt".equals(headerType)) {
				throw error("JWT proof validation failed: Invalid JWT type (typ)", args("jwt", jwt,
					"expected", "openid4vci-proof+jwt",
					"actual", headerType));
			}
			log("Found expected proof jwt type", args("header", headerType));


			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				throw error("JWT proof validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256.", args("jwt", jwt, "alg", header.getAlgorithm()));
			}
			log("Found expected proof jwt algorithm", args("algorithm", header.getAlgorithm()));

			JWK walletPublicKey = extractPublicJwkFromProofJWTHeader(header, jwt, publicKeysJwks);

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
			log("Detected valid proof JWT");

			// 5. Validate Claims (No changes needed in claim checking logic itself)
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// Check Audience
			List<String> audiences = claimsSet.getAudience();
			if (audiences == null || !audiences.contains(audience)) {
				throw error("JWT proof validation failed: Invalid or missing audience (aud) claim. Expected audience to contain: " + audience, args("aud", audiences));
			}
			log("Detected expected issuer in audience", args("aud", audiences, "expected_audience", audience));

			// Check Nonce
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
			log("Detected JWT proof has issued within the last 5 minutes", args("iat", issueTime.toInstant()));

			// 6. Validation successful :)
			logSuccess("Successfully validated proof jwt", args("jwt", jwt, "claims", claimsSet));

		} catch (JOSEException e) {
			throw error("JWT proof validation failed: JOSE error during validation", e);
		} catch (Exception e) {
			throw error("JWT proof validation failed: Unexpected error during validation", e);
		}

		return env;
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
