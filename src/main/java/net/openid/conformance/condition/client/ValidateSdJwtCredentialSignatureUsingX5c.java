package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;

/**
 * Validates the SD-JWT VC issuer signature using the leaf certificate from the x5c header.
 *
 * This is used for HAIP, where the credential carries the validation key in x5c.
 */
public class ValidateSdJwtCredentialSignatureUsingX5c extends EnsureX5cHeaderPresentForSdJwtCredential {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {
		super.evaluate(env);

		String credentialJwt = env.getString("sdjwt", "credential.value");
		JsonElement x5cElement = env.getElementFromObject("sdjwt", "credential.header.x5c");
		List<String> x5c = OIDFJSON.convertJsonArrayToList(x5cElement.getAsJsonArray());

		String encodedLeafCert = x5c.get(0);
		byte[] leafCertDer = Base64.getDecoder().decode(encodedLeafCert);
		X509Certificate leafCert = X509CertUtils.parse(leafCertDer);

		if (leafCert == null) {
			throw error("Failed to parse leaf certificate from credential x5c header",
				args("x5c", x5c, "leaf_cert_encoded", encodedLeafCert));
		}

		try {
			SignedJWT jwt = SignedJWT.parse(credentialJwt);
			PublicKey publicKey = leafCert.getPublicKey();
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), publicKey);

			if (!jwt.verify(verifier)) {
				throw error("Credential signature could not be verified using the leaf certificate from x5c",
					args("credential", credentialJwt,
						"leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
			}
		} catch (ParseException e) {
			throw error("Failed to parse credential JWT for signature verification", e,
				args("credential", credentialJwt));
		} catch (JOSEException e) {
			throw error("Error verifying credential signature using x5c", e,
				args("credential", credentialJwt,
					"leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
		}

		logSuccess("Credential signature verified successfully using the leaf certificate from x5c",
			args("credential", credentialJwt,
				"leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));

		return env;
	}
}
