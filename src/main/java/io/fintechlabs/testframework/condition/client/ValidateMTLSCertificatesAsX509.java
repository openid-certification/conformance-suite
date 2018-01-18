package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

		Security.addProvider(new BouncyCastleProvider());
		CertificateFactory certFactory = null;
		try {

			certFactory = CertificateFactory.getInstance("X.509","BC");
			X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(certString)));
			String modifiedKeyString = "-----BEGIN RSA PRIVATE KEY-----\n" +
					new String(Base64.getDecoder().decode(keyString)) +
					"\n-----END RSA PRIVATE KEY-----";
			KeyStore keystore = KeyStore.getInstance("PKCS12", "BC");

			// This might work for the keyPassword?
			char[] keyPassword = {};
			// keystore.load(new ByteArrayInputStream(modifiedKeyString.getBytes()), keyPassword);
			keystore.load(new ByteArrayInputStream(Base64.getDecoder().decode(keyString)), keyPassword);


		} catch (CertificateException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (NoSuchProviderException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (KeyStoreException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (NoSuchAlgorithmException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		} catch (IOException e) {
			return error("Couldn't validate certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}


		logSuccess("Mutual TLS authentication cert validated as X.509");
		return env;
	}
}
