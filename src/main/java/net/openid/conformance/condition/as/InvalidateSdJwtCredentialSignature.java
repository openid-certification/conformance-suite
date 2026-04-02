package net.openid.conformance.condition.as;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

/**
 * Invalidates the issuer JWT signature in an SD-JWT+KB credential while keeping the KB-JWT
 * sd_hash correct. This ensures the test isolates issuer signature verification rather than
 * also triggering an sd_hash mismatch.
 */
public class InvalidateSdJwtCredentialSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = {"credential", "holder_private_jwk"})
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		String credential = env.getString("credential");

		// Split into: sdJwtWithoutKb (issuerJwt~disc1~...~) and kbJwt
		int lastTilde = credential.lastIndexOf('~');
		if (lastTilde < 0 || lastTilde == credential.length() - 1) {
			throw error("SD-JWT credential does not contain a key binding JWT",
				args("credential", credential));
		}
		String sdJwtWithoutKb = credential.substring(0, lastTilde + 1);
		String kbJwt = credential.substring(lastTilde + 1);

		int firstTilde = sdJwtWithoutKb.indexOf('~');
		if (firstTilde < 0) {
			throw error("SD-JWT credential does not contain any disclosures",
				args("credential", credential));
		}
		String issuerJwt = sdJwtWithoutKb.substring(0, firstTilde);
		String disclosureSuffix = sdJwtWithoutKb.substring(firstTilde);

		// Invalidate the issuer JWT signature
		String invalidIssuerJwt = invalidateSignatureString("credential", issuerJwt);
		String invalidSdJwtWithoutKb = invalidIssuerJwt + disclosureSuffix;

		// Recompute sd_hash over the corrupted SD-JWT and re-sign the KB-JWT
		String newSdHash;
		try {
			newSdHash = ValidateSdJwtKbSdHash.getCalculatedSdHash(invalidSdJwtWithoutKb);
		} catch (NoSuchAlgorithmException e) {
			throw error("Failed to compute sd_hash", e);
		}

		JWTClaimsSet originalKbClaims;
		try {
			originalKbClaims = SignedJWT.parse(kbJwt).getJWTClaimsSet();
		} catch (ParseException e) {
			throw error("Failed to parse KB-JWT", e, args("kbJwt", kbJwt));
		}

		ECKey holderKey;
		try {
			holderKey = ECKey.parse(env.getString("holder_private_jwk"));
		} catch (ParseException e) {
			throw error("Failed to parse holder private key", e);
		}

		JWTClaimsSet newKbClaims = new JWTClaimsSet.Builder(originalKbClaims)
			.claim("sd_hash", newSdHash)
			.build();
		JWSHeader kbHeader = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("kb+jwt"))
			.build();
		SignedJWT newKbJwt = new SignedJWT(kbHeader, newKbClaims);
		try {
			newKbJwt.sign(new ECDSASigner(holderKey));
		} catch (JOSEException e) {
			throw error("Failed to sign KB-JWT", e);
		}

		env.putString("credential", invalidSdJwtWithoutKb + newKbJwt.serialize());

		log("Invalidated issuer signature in SD-JWT credential and recomputed KB-JWT sd_hash");

		return env;
	}
}
