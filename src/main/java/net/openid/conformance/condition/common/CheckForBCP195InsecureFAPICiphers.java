package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.NoopTlsAuthentication;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsClient;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;


import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cipher suites that are not recommended in the <a href="https://www.rfc-editor.org/info/bcp195">BCP195</a> are considered insecure for FAPI usage.
 * See <a href="https://bitbucket.org/openid/fapi/issues/685/use-of-tls-12-ciphers#comment-66826146">Vulnerability in TLS_DHE_RSA_WITH_AES_128_GCM_SHA256</a>
 */
@SuppressWarnings("deprecation")
public class CheckForBCP195InsecureFAPICiphers extends AbstractCheckInsecureCiphers {

	/**
	 * This map contains the cipher suites, which should produce a warning when detected.
	 */
	private static final Map<Integer, String> INSECURE_CIPHERS = Map.ofEntries(
		Map.entry(CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, "DHE_RSA_WITH_AES_128_GCM_SHA256"),
		Map.entry(CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, "DHE_RSA_WITH_AES_256_GCM_SHA384")
	);

	@Override
	Map<Integer, String> getInsecureCiphers() {
		return INSECURE_CIPHERS;
	}

}
