package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;

/**
 * Validates the client attestation JWT signature using the public key from the x5c header.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
public class ValidateClientAttestationSignature extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_attestation_object", strings = "client_attestation")
	public Environment evaluate(Environment env) {

		String clientAttestation = env.getString("client_attestation");

		// Get the x5c header from the client attestation
		JsonElement x5cEl = env.getElementFromObject("client_attestation_object", "header.x5c");
		if (x5cEl == null || !x5cEl.isJsonArray() || x5cEl.getAsJsonArray().isEmpty()) {
			throw error("Client attestation does not have x5c header or x5c is empty",
				args("client_attestation", clientAttestation));
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(x5cEl.getAsJsonArray());
		String encodedLeafCert = x5c.get(0);

		// Parse the leaf certificate
		byte[] leafCertDer = Base64.getDecoder().decode(encodedLeafCert);
		X509Certificate leafCert = X509CertUtils.parse(leafCertDer);
		if (leafCert == null) {
			throw error("Failed to parse leaf certificate from x5c header",
				args("x5c", x5cEl, "encoded_leaf_cert", encodedLeafCert));
		}

		// Get the public key from the leaf certificate
		PublicKey publicKey = leafCert.getPublicKey();

		try {
			// Parse the JWT
			SignedJWT jwt = SignedJWT.parse(clientAttestation);

			// Create a verifier based on the key type
			JWSVerifier verifier;
			if (publicKey instanceof ECPublicKey ecPublicKey) {
				verifier = new ECDSAVerifier(ecPublicKey);
			} else if (publicKey instanceof RSAPublicKey rsaPublicKey) {
				verifier = new RSASSAVerifier(rsaPublicKey);
			} else {
				throw error("Unsupported public key type in x5c certificate",
					args("key_type", publicKey.getClass().getName()));
			}

			// Verify the signature
			if (!jwt.verify(verifier)) {
				throw error("Client attestation JWT signature verification failed. " +
					"The signature does not match the public key in the x5c header.",
					args("client_attestation", clientAttestation,
						"leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
			}

			logSuccess("Client attestation JWT signature verified successfully using x5c public key",
				args("client_attestation", clientAttestation,
					"leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));

		} catch (ParseException e) {
			throw error("Failed to parse client attestation JWT", e,
				args("client_attestation", clientAttestation));
		} catch (JOSEException e) {
			throw error("Error verifying client attestation JWT signature", e,
				args("client_attestation", clientAttestation));
		}

		return env;
	}
}
