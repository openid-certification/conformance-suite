package net.openid.conformance.condition.util;

import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;


/**
 * This class implements a KeyManager to be used by requests to MTLS endpoints.
 */
public class DelegatedClientKeyManager extends X509ExtendedKeyManager {

	static final String ALIAS = "dummyalias";
	static final String[] ALIASES = new String[]{ALIAS};

	private final X509Certificate[] chain;

	private final RSAPrivateKey key;

	public DelegatedClientKeyManager(X509Certificate cert, RSAPrivateKey key) {
		this.chain = new X509Certificate[]{cert};
		this.key = key;
	}

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return ALIASES;
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		return ALIAS;
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return ALIASES;
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return ALIAS;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		return chain;
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		return key;
	}
}
