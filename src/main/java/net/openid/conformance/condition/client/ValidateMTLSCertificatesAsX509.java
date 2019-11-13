package net.openid.conformance.condition.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;

import static java.util.stream.Collectors.toCollection;

public class ValidateMTLSCertificatesAsX509 extends AbstractCondition {

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

		X509Certificate certificate = generateCertificateFromMTLSCert(certString, certFactory);

		validateMTLSKey(certString, keyString, caString, certificate);

		if (!Strings.isNullOrEmpty(caString)) {
			validateMTLSCa(env, certFactory, caString);
		}

		logSuccess("Mutual TLS authentication cert validated as X.509");

		return env;
	}

	private X509Certificate generateCertificateFromMTLSCert(String certString, CertificateFactory certFactory) {
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
		return certificate;
	}

	private void validateMTLSKey(String certString, String keyString, String caString, X509Certificate certificate) {
		byte[] decodedKey;
		try {
			decodedKey = Base64.getDecoder().decode(keyString);
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of key failed", e, args("key", keyString));
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
	}

	private void validateMTLSCa(Environment env, CertificateFactory certFactory, String caString) {
		byte[] decodedCa;
		try {
			decodedCa = Base64.getDecoder().decode(caString);
			Collection<? extends Certificate> caCertificateChain = certFactory.generateCertificates(new ByteArrayInputStream(decodedCa));
			ArrayList<Certificate> caCertificateChainList = caCertificateChain.stream().collect(toCollection(ArrayList::new));

			boolean isWrongOrder = false;
			for (int i = 0; i < caCertificateChainList.size(); i++) {
				X509Certificate x509Certificate = (X509Certificate) caCertificateChainList.get(i);
				if (isSelfSigned(x509Certificate) && i < caCertificateChainList.size() - 1) {
					caCertificateChainList.remove(i);
					caCertificateChainList.add(x509Certificate);
					isWrongOrder = true;
					break;
				}
			}

			if (isWrongOrder) {
				log("Root & issuing in mtls.ca is wrong order. Automatically correct it (Issuing first, then root)");

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				for (Certificate certificate : caCertificateChainList) {
					out.write(certificate.getEncoded(), 0, certificate.getEncoded().length);
				}

				String newCaString = Base64.getEncoder().encodeToString(out.toByteArray());
				JsonObject mtls = env.getObject("mutual_tls_authentication");
				mtls.addProperty("ca", newCaString);
				env.putObject("mutual_tls_authentication", mtls);
			}
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of ca failed", e, args("ca", caString));
		} catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
			throw error("Couldn't validate ca cert", e, args("ca", caString));
		}
	}

	private boolean isSelfSigned(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		try {
			// Try to verify certificate signature with its own public key
			PublicKey key = cert.getPublicKey();
			cert.verify(key);
			return true;
		} catch (SignatureException | InvalidKeyException e) {
			return false;
		}
	}
}
