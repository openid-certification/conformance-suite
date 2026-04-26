package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.SignedJWT;

import java.security.PublicKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;

/**
 * Non-HAIP fallback signature verification for key attestation JWTs that arrive without
 * an x5c JOSE header. Uses the configured {@code vci.key_attestation_jwks} JWKS.
 *
 * Skips silently when the signature has already been verified by
 * {@link ValidateKeyAttestationX5cCertificateChain} (which sets
 * {@code key_attestation_signature_verified=true} in env).
 */
public class VerifyKeyAttestationSignatureUsingConfigJwks extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		Boolean alreadyVerified = env.getBoolean("key_attestation_signature_verified");
		if (Boolean.TRUE.equals(alreadyVerified)) {
			log("Key attestation signature already verified using x5c leaf certificate, skipping JWKS fallback");
			return env;
		}

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		String rawJwt = OIDFJSON.getString(keyAttestationJwt.get("value"));

		JsonElement keyAttestationJwksEl = env.getElementFromObject("config", "vci.key_attestation_jwks");
		if (keyAttestationJwksEl == null) {
			String errorDescription = "'Key Attestation JWKS' field is missing from the 'Key Attestation' section in the test configuration";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription);
		}

		try {
			JWK walletPublicKey = JWKUtil.getSigningKey(keyAttestationJwksEl.getAsJsonObject());
			if (!(walletPublicKey instanceof AsymmetricJWK asymmetricKey)) {
				String errorDescription = "Key attestation signing key in 'Key Attestation JWKS' is not an asymmetric key";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("kty", walletPublicKey.getKeyType()));
			}
			PublicKey publicKey = asymmetricKey.toPublicKey();

			SignedJWT signedJwt = SignedJWT.parse(rawJwt);
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(signedJwt.getHeader(), publicKey);
			if (!signedJwt.verify(verifier)) {
				String errorDescription = "Key attestation JWT signature verification failed using the configured JWKS";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription);
			}
		} catch (ParseException e) {
			String errorDescription = "Failed to parse key attestation JWT or configured JWKS for signature verification";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e);
		} catch (JOSEException e) {
			String errorDescription = "Error verifying key attestation JWT signature using configured JWKS";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e);
		}

		env.putBoolean("key_attestation_signature_verified", true);
		logSuccess("Verified key attestation JWT signature using the configured JWKS");
		return env;
	}
}
