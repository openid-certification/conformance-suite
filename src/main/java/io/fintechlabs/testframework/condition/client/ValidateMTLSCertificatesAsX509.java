package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class ValidateMTLSCertificatesAsX509 extends AbstractCondition {

	/**
	 *
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ValidateMTLSCertificatesAsX509(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		String certString = env.getString("mutual_tls_authentication", "cert");
		String keyString = env.getString("mutual_tls_authentication", "key");
		String caString = env.getString("mutual_tls_authentication", "ca");

		if (Strings.isNullOrEmpty(certString) || Strings.isNullOrEmpty(keyString)) {
			return error("Couldn't find TLS client certificate or key for MTLS");
		}

		Security.addProvider(new BouncyCastleProvider());
		CertificateFactory certFactory = null;
		try {

			certFactory = CertificateFactory.getInstance("X.509","BC");
			X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(certString)));

			KeyFactory kf = KeyFactory.getInstance("RSA","BC");
			KeySpec kspec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyString));
			RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(kspec);


			// Check that the private key and the certificate match
			RSAPublicKey publicKey = (RSAPublicKey)certificate.getPublicKey();
			if (!(privateKey.getModulus().equals(publicKey.getModulus()))){
				return error("MTLS Private Key and Cert do not match",args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
			}

			if (!Strings.isNullOrEmpty(caString)) {
				X509Certificate caCertificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(caString)));
			}

		} catch (CertificateException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (NoSuchProviderException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (NoSuchAlgorithmException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (InvalidKeySpecException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (IllegalArgumentException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}


		logSuccess("Mutual TLS authentication cert validated as X.509");
		return env;
	}
}
