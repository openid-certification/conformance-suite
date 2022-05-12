package net.openid.conformance.condition.util;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

import javax.net.ssl.KeyManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class MtlsKeystoreBuilder {

	private static KeystoreStrategy defaultStrategy = new DefaultMtlsStrategy();
	private static KeystoreStrategy kmsStrategy = new AwsKmsMtlsStrategy();

	public static KeyManager[] configureMtls(Environment env) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
		JsonObject mtls  = env.getObject("mutual_tls_authentication");
		if(!mtls.has("alias")) {
			return defaultStrategy.process(env);
		} else {
			return kmsStrategy.process(env);
		}
	}


	protected static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

		KeyFactory factory = KeyFactory.getInstance("RSA");

		return (RSAPrivateKey) factory.generatePrivate(spec);
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
