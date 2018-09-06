package io.fintechlabs.testframework.condition.client;

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

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateMTLSCertificatesAsX509 extends AbstractCondition {

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
			throw error("Couldn't find TLS client certificate or key for MTLS");
		}

		Security.addProvider(new BouncyCastleProvider());
		CertificateFactory certFactory = null;
		try {
			certFactory = CertificateFactory.getInstance("X.509", "BC");
		} catch (CertificateException | NoSuchProviderException | IllegalArgumentException e) {
			throw error("Couldn't get CertificateFactory", e);
		}

		byte[] decodedKey;
		try {
			decodedKey = Base64.getDecoder().decode(keyString);
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of key failed", e, args("key", keyString));
		}

		byte[] decodedCert;
		try {
			decodedCert = Base64.getDecoder().decode(certString);
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of cert failed", e, args("cert", certString));
		}

		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedCert));
		} catch (CertificateException | IllegalArgumentException e) {
			throw error("Calling generateCertificate on cert failed", e, args("cert", certString));
		}

		KeyFactory kf;
		try {
			kf = KeyFactory.getInstance("RSA", "BC");
		} catch (NoSuchProviderException | NoSuchAlgorithmException | IllegalArgumentException e) {
			throw error("Couldn't get KeyFactory", e);
		}

		RSAPrivateKey privateKey;
		try {
			KeySpec kspec = new PKCS8EncodedKeySpec(decodedKey);
			privateKey = (RSAPrivateKey) kf.generatePrivate(kspec);
		} catch (InvalidKeySpecException | IllegalArgumentException e) {
			throw error("Couldn't validate private key", e, args("key", keyString));
		}

		// Check that the private key and the certificate match
		RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
		if (!(privateKey.getModulus().equals(publicKey.getModulus()))) {
			throw error("MTLS Private Key and Cert do not match", args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}

		if (!Strings.isNullOrEmpty(caString)) {
			byte[] decodedCa;
			try {
				decodedCa = Base64.getDecoder().decode(caString);
			} catch (IllegalArgumentException e) {
				throw error("base64 decode of ca failed", e, args("ca", caString));
			}

			try {
				X509Certificate caCertificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedCa));
			} catch (CertificateException | IllegalArgumentException e) {
				throw error("Calling generateCertificate on ca failed", e, args("ca", caString));
			}
		}

		logSuccess("Mutual TLS authentication cert validated as X.509");
		return env;
	}
}
