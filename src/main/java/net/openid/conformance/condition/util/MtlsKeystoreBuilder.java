package net.openid.conformance.condition.util;

import com.google.common.collect.Lists;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MtlsKeyUtil;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MtlsKeystoreBuilder {

	public static KeyManager[] configureMtls(Environment env) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, InvalidKeySpecException {
		String clientCert = env.getString("mutual_tls_authentication", "cert");
		String clientKey = env.getString("mutual_tls_authentication", "key");
		String clientCa = env.getString("mutual_tls_authentication", "ca");

		byte[] certBytes = Base64.getDecoder().decode(clientCert);
		byte[] keyBytes = Base64.getDecoder().decode(clientKey);

		X509Certificate cert = generateCertificateFromDER(certBytes);

		// use public key from cert to aid in decoding the private key
		// which can be EC or RSA keys in PKCS#1 or PKCS#8 format
		PublicKey publicKey = cert.getPublicKey();
		String alg = publicKey.getAlgorithm();

		PrivateKey key = MtlsKeyUtil.generateAlgPrivateKeyFromDER(alg, keyBytes);

		ArrayList<X509Certificate> chain = Lists.newArrayList(cert);
		if (clientCa != null) {
			byte[] caBytes = Base64.getDecoder().decode(clientCa);
			chain.addAll(generateCertificateChainFromDER(caBytes));
		}

		KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(null);
		keystore.setCertificateEntry("cert-alias", cert);
		keystore.setKeyEntry("key-alias", key, "changeit".toCharArray(), chain.toArray(new Certificate[chain.size()]));

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keystore, "changeit".toCharArray());

		return  keyManagerFactory.getKeyManagers();
	}


	protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
	}

	protected static List<X509Certificate> generateCertificateChainFromDER(byte[] chainBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		ArrayList<X509Certificate> chain = new ArrayList<>();
		ByteArrayInputStream in = new ByteArrayInputStream(chainBytes);
		while (in.available() > 0) {
			chain.add((X509Certificate) factory.generateCertificate(in));
		}

		return chain;
	}

}
