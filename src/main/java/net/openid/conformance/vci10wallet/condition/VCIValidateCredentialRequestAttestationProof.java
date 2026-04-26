package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

public class VCIValidateCredentialRequestAttestationProof extends AbstractVCIValidateCredentialRequestProof {

	private static final String VCI_HAIP_PROFILE = "vci_haip";

	@Override
	protected void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {
		// For proof_type=attestation, this condition only parses the JWT and stores it in env.
		// Subsequent validation (typ, alg, x5c, signature, claims) is performed by separate
		// conditions wired into AbstractVCIWalletTest.handleCredentialRequest.
		// The validateKeyAttestation(...) method below is retained for use by the nested
		// key-attestation case in VCIValidateCredentialRequestJwtProof.
		String attestationJwt = env.getString("proof_attestation", "value");
		try {
			env.putObject("vci", "key_attestation_jwt", JWTUtil.jwtStringToJsonObjectForEnvironment(attestationJwt));
		} catch (ParseException e) {
			String errorDescription = "Key attestation validation of proof failed: could not parse JWT";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e, args("attestation", attestationJwt));
		}
		logSuccess("Parsed key attestation JWT for proof type: " + proofType, args("proof_type", proofType));
	}

	protected void validateKeyAttestation(Environment env, String proofType, String attestationJwt) {
		try {
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(attestationJwt);
			JWSHeader header = signedJWT.getHeader();

			String headerType = header.getType().getType();
			if (!"key-attestation+jwt".equals(headerType)) {
				String errorDescription = "Key attestation validation of proof failed: Invalid JWT type (typ)";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("attestation", attestationJwt, "expected", "key-attestation+jwt", "actual", headerType));
			}
			log("Found expected typ '" +  headerType+ "' in header of key attestation for proof type " + proofType, args("header", headerType, "proof_type", proofType));
			env.putObject("vci","key_attestation_jwt", JWTUtil.jwtStringToJsonObjectForEnvironment(attestationJwt));

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				String errorDescription = "Attestation validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("jwt", attestationJwt, "alg", header.getAlgorithm()));
			}
			log("Found expected algorithm for Key attestation in proof type: " + proofType, args("algorithm", header.getAlgorithm()));

			JWK walletPublicKey = resolveKeyAttestationSigningKey(env, attestationJwt, header);

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				String errorDescription = "key Attestation validation failed: Key found but is not an ECKey for kid: " + header.getKeyID();
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription);
			}
			log("Detected EC public key", args("kid", header.getKeyID()));

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				String errorDescription = "Key Attestation validation failed: Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("curve", ecPublicKey.getCurve().getName()));
			}
			log("Detected EC public key with curve P-256", args("kid", header.getKeyID()));

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				String errorDescription = "Key Attestation validation failed: JWT signature validation failed";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription);
			}
			log("Detected valid Key Attestation for proof type: " + proofType);

			// 5. Validate Claims
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			JsonObject keyAttestationVerifiableObject = new JsonObject();
			keyAttestationVerifiableObject.addProperty("verifiable_jws", attestationJwt);
			keyAttestationVerifiableObject.addProperty("public_jwk", walletPublicKey.toString());

			checkNonceIfNecessary(env, claimsSet);

			log("Detected key attestation in 'jwt' proof header",
				args("key_attestation_jwt", keyAttestationVerifiableObject));

			// 6. Validation successful :)
			logSuccess("Successfully validated key attestation for proof type: " + proofType, args("attestation_jwt", keyAttestationVerifiableObject, "claims", claimsSet));

		} catch (JOSEException e) {
			String errorDescription = "Attestation validation failed: JOSE error during validation of proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e);
		} catch (ConditionError e) {
			// Re-throw ConditionError without wrapping - the error response is already set
			throw e;
		} catch (Exception e) {
			String errorDescription = "Attestation validation failed: Unexpected error during validation of proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, e);
		}
	}

	protected JWK resolveKeyAttestationSigningKey(Environment env, String attestationJwt, JWSHeader header) throws ParseException {
		// Per HAIP §4.5.1 / RFC 7515 §4.1.6, when x5c is present in the JWS header,
		// it carries the key used to sign the JWT. Validate the chain and verify against
		// the leaf certificate. Non-HAIP requests without x5c fall back to configured JWKS.
		List<Base64> x5cChain = header.getX509CertChain();
		if (x5cChain != null) {
			if (x5cChain.isEmpty()) {
				String errorDescription = "Key attestation JWT header x5c claim MUST NOT be empty";
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription, args("jwt", attestationJwt, "header", header.toJSONObject()));
			}
			validateKeyAttestationX5cCertificateChain(env, x5cChain);
			return extractPublicJwkFromX5c(env, attestationJwt, x5cChain);
		}

		if (isHaipProfile(env)) {
			String errorDescription = "Key attestation JWT header MUST contain an x5c claim per HAIP §4.5.1";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("jwt", attestationJwt, "header", header.toJSONObject()));
		}

		JsonElement keyAttestationJwksEl = env.getElementFromObject("config", "vci.key_attestation_jwks");
		if (keyAttestationJwksEl == null) {
			throw error("'Key Attestation JWKS' field is missing from the 'Key Attestation' section in the test configuration");
		}
		return JWKUtil.getSigningKey(keyAttestationJwksEl.getAsJsonObject());
	}

	private boolean isHaipProfile(Environment env) {
		return VCI_HAIP_PROFILE.equals(env.getString("vci", "fapi_profile"));
	}

	private void validateKeyAttestationX5cCertificateChain(Environment env, List<Base64> x5cChain) {
		try {
			List<X509Certificate> certs = parseX5cCertificatesFromNimbusBase64(x5cChain);

			String trustAnchorPem = env.getString("vci", "key_attestation_trust_anchor_pem");
			X509Certificate trustAnchorCert = trustAnchorPem != null ? X509CertUtils.parse(trustAnchorPem) : null;

			validateX5cCertificateChain(certs, trustAnchorCert);

			logSuccess("Validated key attestation x5c certificate chain",
				args("x5c", x5cChain,
					"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
					"chain_length", certs.size()));
		} catch (ConditionError e) {
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF,
				"Key attestation x5c certificate chain validation failed");
			throw e;
		}
	}

	protected void checkNonceIfNecessary(Environment env, JWTClaimsSet claimsSet) throws ParseException {

		// do we see a nonce in the key attestation?
		String nonce = claimsSet.getStringClaim("nonce");

		// Did we generate a c_nonce?
		JsonObject credentialNonceResponse = env.getObject("credential_nonce_response");
		if (credentialNonceResponse == null || !credentialNonceResponse.has("c_nonce")) {
			if (nonce != null) {
				log("Found unexpected nonce in key attestation, but the nonce endpoint was not called prior.");
			}
			return;
		}
		// We did generate a c_nonce
		String expectedNonce = OIDFJSON.getString(credentialNonceResponse.get("c_nonce"));

		if (nonce == null) {
			String errorDescription = "Key attestation did not contain a nonce value, but the nonce endpoint was called prior.";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_NONCE, errorDescription);
			throw error(errorDescription,
				args("c_nonce", expectedNonce));
		}

		if (!expectedNonce.equals(nonce)) {
			String errorDescription = "Key attestation did not contain the expected nonce value";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_NONCE, errorDescription);
			throw error(errorDescription,
				args("nonce", nonce, "c_nonce", expectedNonce));
		}

		// remove generated credential_nonce_response
		env.removeObject("credential_nonce_response");
		log("Detected and invalidated expected nonce value in Key attestation ",
			args("nonce", nonce, "c_nonce", expectedNonce));
	}
}
