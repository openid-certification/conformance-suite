package net.openid.conformance.condition.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMtlsStrategy implements KeystoreStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractMtlsStrategy.class);
	private static Map<String, KeystoreStrategy> mtlsStrategies = new HashMap<>();

	protected RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

		KeyFactory factory = KeyFactory.getInstance("RSA");

		return (RSAPrivateKey) factory.generatePrivate(spec);
	}

	protected X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
	}

	protected List<X509Certificate> generateCertificateChainFromDER(byte[] chainBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		ArrayList<X509Certificate> chain = new ArrayList<>();
		ByteArrayInputStream in = new ByteArrayInputStream(chainBytes);
		while (in.available() > 0) {
			chain.add((X509Certificate) factory.generateCertificate(in));
		}

		return chain;
	}

	public static void register(String providerName, KeystoreStrategy strategy) {
		LOG.info("Keystore strategy {} registered. ({})", providerName, strategy.getClass().getName());
		mtlsStrategies.put(providerName, strategy);
	}

	public static KeystoreStrategy forName(String name) {
		LOG.info("Looking up keystore strategy {}", name);
		if(mtlsStrategies.containsKey(name)) {
			return mtlsStrategies.get(name);
		}
		throw new RuntimeException("No alternate mtls strategy called " + name + " is configured");
	}

}
