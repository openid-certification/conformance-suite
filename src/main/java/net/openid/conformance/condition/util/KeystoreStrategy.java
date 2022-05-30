package net.openid.conformance.condition.util;

import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public interface KeystoreStrategy {

	KeyManager[] process(Environment env, TestInstanceEventLog log) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException;

}
