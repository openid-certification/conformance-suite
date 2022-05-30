package net.openid.conformance.condition.util;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

public class AwsKmsMtlsStrategy extends AbstractMtlsStrategy {
	@Override
	public KeyManager[] process(Environment env, TestInstanceEventLog log) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
		String clientCert = env.getString("mutual_tls_authentication", "cert");
		String clientCa = env.getString("mutual_tls_authentication", "ca");
		JsonObject alt = env.getElementFromObject("mutual_tls_authentication", "alias").getAsJsonObject();
		String keyAlias = OIDFJSON.getString(alt.get("key"));
		log.log("AwsKmsMtlsStrategy", Map.of("KMS alias used for mtls", keyAlias));

		byte[] certBytes = Base64.getDecoder().decode(clientCert);

		X509Certificate cert = generateCertificateFromDER(certBytes);

		ArrayList<X509Certificate> chain = Lists.newArrayList(cert);
		if (clientCa != null) {
			byte[] caBytes = Base64.getDecoder().decode(clientCa);
			chain.addAll(generateCertificateChainFromDER(caBytes));
		}

		KeyStore keystore = KeyStore.getInstance("KMS");
		keystore.load(null);
		keystore.setCertificateEntry(keyAlias, cert);

	    verify(cert.getPublicKey(), keystore.getKey(keyAlias, null));

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keystore, null);

		return keyManagerFactory.getKeyManagers();
	}

	private boolean verify(Key publicKey, Key privateKey) {
		RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
		RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
		BigInteger privateModulus = rsaPrivateKey.getModulus();
		BigInteger publicModulus = rsaPublicKey.getModulus();

		return privateModulus.equals(publicModulus);

	}

}
