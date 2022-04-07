package net.openid.conformance.condition.rs;

import net.openid.conformance.extensions.AbstractKeystoreStrategy;
import net.openid.conformance.extensions.AlternateKeystoreRegistry;
import net.openid.conformance.extensions.KeystoreStrategy;
import net.openid.conformance.testmodule.Environment;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * A simple implementation of the alternate keystore strategy.
 * This does nothing other than return the kmf from a given keystore
 */
public class SimpleAlternateKeystoreStrategy implements KeystoreStrategy {

	private final KeyStore keystore;

	public SimpleAlternateKeystoreStrategy(KeyStore alternateKeyStre) {
		this.keystore = alternateKeyStre;
	}

	@Override
	public KeyManager[] process(Environment env) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keystore, "changeit".toCharArray());
		return kmf.getKeyManagers();
	}
}
