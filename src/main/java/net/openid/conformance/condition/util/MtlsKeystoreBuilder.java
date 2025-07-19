package net.openid.conformance.condition.util;

import com.google.common.collect.Lists;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
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
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
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

		PrivateKey key = generateAlgPrivateKeyFromDER(alg, keyBytes);

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


	protected static PrivateKey generateAlgPrivateKeyFromDER(String alg, byte[] keyBytes) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		try {
			// try to generate private key using PKCS8, works for both RSA and EC and Ed25519 alg
			// RSA alg will handle both PKCS1 and PKCS8 format here
			// EC alg will throw exception for PKCS1, Ed25519 not possible with PKCS1
			KeySpec kspec = new PKCS8EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance(alg, BouncyCastleProviderSingleton.getInstance()).generatePrivate(kspec);
		} catch (InvalidKeySpecException e) {
			if("EC".equals(alg)) {
				// try to generate private key using PKCS1, code from ValidateMTLSCertificatesAsX509.generateAlgPrivateKeyFromDER
				ASN1Sequence seq = ASN1Sequence.getInstance(keyBytes);
				org.bouncycastle.asn1.sec.ECPrivateKey pKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(seq);
				AlgorithmIdentifier algId = new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, pKey.getParametersObject());
				byte[] server_pkcs8 = new PrivateKeyInfo(algId, pKey).getEncoded();
				return KeyFactory.getInstance(alg, BouncyCastleProviderSingleton.getInstance()).generatePrivate(new PKCS8EncodedKeySpec(server_pkcs8));
			}
			throw e;
		}
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
