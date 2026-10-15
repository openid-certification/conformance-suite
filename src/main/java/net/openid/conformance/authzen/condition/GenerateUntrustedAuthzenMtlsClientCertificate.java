package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Builds a client certificate that a PDP authenticating PEPs by mTLS must refuse (Section 11.3).
 * It keeps the subject, issuer, serial number and validity of the configured certificate but
 * carries a new key and is signed by that key, so no CA vouches for it and its fingerprint
 * differs. The new key has the configured key's type and size or curve (RSA, EC, or EdDSA with
 * Ed25519 or Ed448), so the certificate is invalid only in who issued it.
 */
public class GenerateUntrustedAuthzenMtlsClientCertificate extends AbstractCondition {

	@Override
	@PreEnvironment(required = "mutual_tls_authentication")
	@PostEnvironment(required = "untrusted_mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		String certString = env.getString("mutual_tls_authentication", "cert");
		if (certString == null) {
			throw error("No client certificate to base the untrusted certificate on",
				args("mutual_tls_authentication", env.getObject("mutual_tls_authentication")));
		}

		X509Certificate configured;
		try {
			configured = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(certString)));
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw error("The client certificate could not be parsed", e, args("cert", certString));
		}

		PublicKey configuredKey = configured.getPublicKey();
		KeyPair keyPair;
		String signatureAlgorithm;
		try {
			if (configuredKey instanceof RSAPublicKey rsa) {
				KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
				generator.initialize(rsa.getModulus().bitLength());
				keyPair = generator.generateKeyPair();
				signatureAlgorithm = "SHA256withRSA";
			} else if (configuredKey instanceof ECPublicKey ec) {
				KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
				generator.initialize(ec.getParams());
				keyPair = generator.generateKeyPair();
				signatureAlgorithm = "SHA256withECDSA";
			} else if (configuredKey instanceof EdECPublicKey ed) {
				// The parameter set name ("Ed25519" or "Ed448") is both the key pair and the signature algorithm.
				String curve = ed.getParams().getName();
				keyPair = KeyPairGenerator.getInstance(curve).generateKeyPair();
				signatureAlgorithm = curve;
			} else {
				throw error("The client certificate's key is not RSA, EC or EdDSA, so the test cannot build an untrusted certificate with the same kind of key",
					args("key_algorithm", configuredKey.getAlgorithm()));
			}
		} catch (GeneralSecurityException e) {
			throw error("Generating a key for the untrusted client certificate failed", e);
		}

		X509Certificate untrusted;
		try {
			JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				configured.getIssuerX500Principal(), configured.getSerialNumber(),
				configured.getNotBefore(), configured.getNotAfter(),
				configured.getSubjectX500Principal(), keyPair.getPublic());
			untrusted = new JcaX509CertificateConverter()
				.getCertificate(builder.build(new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate())));
		} catch (OperatorCreationException | GeneralSecurityException e) {
			throw error("Building the untrusted client certificate failed", e);
		}

		JsonObject mtls = new JsonObject();
		try {
			mtls.addProperty("cert", Base64.getEncoder().encodeToString(untrusted.getEncoded()));
		} catch (GeneralSecurityException e) {
			throw error("Encoding the untrusted client certificate failed", e);
		}
		mtls.addProperty("key", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
		env.putObject("untrusted_mutual_tls_authentication", mtls);

		logSuccess("Built a client certificate with the configured subject and issuer, signed by its own new key instead of the issuer",
			args("subject", configured.getSubjectX500Principal().getName(), "issuer", configured.getIssuerX500Principal().getName(),
				"untrusted_mutual_tls_authentication", mtls));
		return env;
	}
}
