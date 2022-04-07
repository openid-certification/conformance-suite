package net.openid.conformance.extensions;

import net.openid.conformance.testmodule.Environment;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * This allows for other implementations of KeyStore to be utilised
 * Useful for providing private keys backed by cloud services, for instance
 */
public interface KeystoreStrategy {

	KeyManager[] process(Environment env) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException;


}
