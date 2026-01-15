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
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

public class VCIValidateCredentialRequestAttestationProof extends AbstractVCIValidateCredentialRequestProof {

	@Override
	protected void validateProof(Environment env, String proofType, String expectedAudience, String credentialConfigurationId, JsonObject credentialConfiguration, JsonObject keyAttestationRequired) {
		String attestationJwt = env.getString("proof_attestation", "value");
		validateKeyAttestation(env, proofType, attestationJwt);
	}

	protected void validateKeyAttestation(Environment env, String proofType, String attestationJwt) {
		try {
			// 1. Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(attestationJwt);
			JWSHeader header = signedJWT.getHeader();

			String headerType = header.getType().getType();
			if (!"key-attestation+jwt".equals(headerType)) {
				throw error("Key attestation validation of proof failed: Invalid JWT type (typ)",
					args("attestation", attestationJwt, "expected", "key-attestation+jwt", "actual", headerType));
			}
			log("Found expected typ '" +  headerType+ "' in header of key attestation for proof type " + proofType, args("header", headerType, "proof_type", proofType));

			if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
				throw error("Attestation validation failed: Unsupported or invalid JWT algorithm (alg). Expected ES256.",
					args("jwt", attestationJwt, "alg", header.getAlgorithm()));
			}
			log("Found expected algorithm for Key attestation in proof type: " + proofType, args("algorithm", header.getAlgorithm()));

			JsonObject keyAttestationJwksObj = env.getElementFromObject("config", "vci.key_attestation_jwks").getAsJsonObject();
			if (keyAttestationJwksObj == null) {
				throw error("Key attestation JWKS is missing in test config. Please add a Key Attestation JWKS with public keys to the test configuration.");
			}

			JWK walletPublicKey = JWKUtil.getSigningKey(keyAttestationJwksObj);

			if (!(walletPublicKey instanceof ECKey ecPublicKey)) {
				throw error("key Attestation validation failed: Key found but is not an ECKey for kid: " + header.getKeyID());
			}
			log("Detected EC public key", args("kid", header.getKeyID()));

			// ensure P_256 curve is used
			if (!Curve.P_256.equals(ecPublicKey.getCurve())) {
				throw error("Key Attestation validation failed: Public key for kid " + header.getKeyID() + " does not use the required P-256 curve.", args("curve", ecPublicKey.getCurve().getName()));
			}
			log("Detected EC public key with curve P-256", args("kid", header.getKeyID()));

			// 3. Create Verifier with the public EC key
			JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

			// 4. Verify the Signature
			if (!signedJWT.verify(verifier)) {
				throw error("Key Attestation validation failed: JWT signature validation failed");
			}
			log("Detected valid Key Attestation for proof type: " + proofType);

			// 5. Validate Claims
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			JsonObject keyAttestationVerifiableObject = new JsonObject();
			keyAttestationVerifiableObject.addProperty("verifiable_jws", attestationJwt);
			keyAttestationVerifiableObject.addProperty("public_jwk", walletPublicKey.toString());

			checkNonceIfNecessary(env, claimsSet);
			checkX5cIfNecessary(env, header);

			log("Detected key attestation in 'jwt' proof header",
				args("key_attestation_jwt", keyAttestationVerifiableObject));

			// 6. Validation successful :)
			logSuccess("Successfully validated key attestation for proof type: " + proofType, args("attestation_jwt", keyAttestationVerifiableObject, "claims", claimsSet));

		} catch (JOSEException e) {
			throw error("Attestation validation failed: JOSE error during validation of proof type: " + proofType, e);
		} catch (Exception e) {
			throw error("Attestation validation failed: Unexpected error during validation of proof type: " + proofType, e);
		}
	}

	protected void checkX5cIfNecessary(Environment env, JWSHeader header) {

		List<Base64> x5c = header.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			log("No x5c claim found in header, skipping certificate checks.");
			return;
		}
		String encodedCert = x5c.get(0).toString();
		X509Certificate keyAttestationCert = X509CertUtils.parse(java.util.Base64.getDecoder().decode(encodedCert));
		String keyAttestationCertPem = X509CertUtils.toPEMString(keyAttestationCert);

		// ensure key attestation cert is valid
		try {
			keyAttestationCert.checkValidity();
		} catch (Exception e) {
			throw error("Certificate used in x5c claim must be valid!",
				args("x5c", encodedCert, "key_attestation_cert_pem", keyAttestationCertPem, "error", e.getMessage()));
		}

		try {
			// ensure key attestation cert is not self-signed
			// see: https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-4.5.1
			keyAttestationCert.verify(keyAttestationCert.getPublicKey());
			throw error("Key attestation cert must not be a self-signed",
				args("cert_0_from_x5c", encodedCert));
		} catch (Exception e) {
			log("Key attestation cert is not a self-signed cert",
				args("cert_0_from_x5c", encodedCert));
		}

		String keyAttestationTrustAnchorPem = env.getString("vci", "key_attestation_trust_anchor_pem");
		if (keyAttestationTrustAnchorPem == null) {
			log("Skipping additional key attestation certificate validation: No trust anchor configured");
			return;
		}

		// validate with key attestation trust anchor if available
		X509Certificate trustAnchorCert = X509CertUtils.parse(keyAttestationTrustAnchorPem);
		try {
			keyAttestationCert.verify(trustAnchorCert.getPublicKey());
		} catch (Exception e) {
			throw error("Certificate used in key attestation must be verifiable by trust anchor certificate.",
				args("x5c", encodedCert, "key_attestation_cert_pem", keyAttestationCertPem, "trust_anchor_pem", keyAttestationTrustAnchorPem, "error", e.getMessage()));
		}
		log("Successfully validated key attestation certificate with trust anchor certificate.",
			args("x5c", encodedCert, "key_attestation_cert_pem", keyAttestationCertPem, "trust_anchor_pem", keyAttestationTrustAnchorPem));
	}

	protected void checkNonceIfNecessary(Environment env, JWTClaimsSet claimsSet) throws ParseException {

		// do we see a nonce in the key attestation?
		String nonce = claimsSet.getStringClaim("nonce");

		// Did we generate a c_nonce?
		JsonObject credentialNonceResponse = env.getObject("credential_nonce_response");
		if (!credentialNonceResponse.has("c_nonce")) {
			if (nonce != null) {
				log("Found unexpected nonce in key attestation, but the nonce endpoint was not called prior.");
			}
			return;
		}
		// We did generate a c_nonce
		String expectedNonce = OIDFJSON.getString(credentialNonceResponse.get("c_nonce"));

		if (nonce == null) {
			throw error("Key attestation did not contain a nonce value, but the nonce endpoint was called prior.",
				args("c_nonce", expectedNonce));
		}

		if (!expectedNonce.equals(nonce)) {
			throw error("Key attestation did not contain the expected nonce value",
				args("nonce", nonce, "c_nonce", expectedNonce));
		}

		log("Found expected nonce value in Key attestation",
			args("nonce", nonce, "c_nonce", expectedNonce));
	}
}
