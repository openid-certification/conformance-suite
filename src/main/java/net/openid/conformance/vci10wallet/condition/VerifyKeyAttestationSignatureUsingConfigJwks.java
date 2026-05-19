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
 * an x5c JOSE header. Uses the configured {@code client_attestation.key_attestation_jwks} JWKS.
 *
 * Skips silently when the JWT carries an x5c header — that case is handled by
 * {@link ValidateKeyAttestationX5cCertificateChain}, which verifies the signature against
 * the leaf certificate's public key.
 */
public class VerifyKeyAttestationSignatureUsingConfigJwks extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject header = keyAttestationJwt.getAsJsonObject("header");
		JsonElement x5cEl = header != null ? header.get("x5c") : null;
		if (x5cEl != null && x5cEl.isJsonArray() && !x5cEl.getAsJsonArray().isEmpty()) {
			log("Key attestation has x5c header — signature was verified against leaf certificate, skipping JWKS fallback");
			return env;
		}

		String rawJwt = OIDFJSON.getString(keyAttestationJwt.get("value"));

		// Read the new key first; fall back to the legacy vci.* key so existing stored
		// test configs keep working through a transition window.
		JsonElement keyAttestationJwksEl = env.getElementFromObject("config", "client_attestation.key_attestation_jwks");
		if (keyAttestationJwksEl == null) {
			keyAttestationJwksEl = env.getElementFromObject("config", "vci.key_attestation_jwks");
		}
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

		logSuccess("Verified key attestation JWT signature using the configured JWKS");
		return env;
	}
}
