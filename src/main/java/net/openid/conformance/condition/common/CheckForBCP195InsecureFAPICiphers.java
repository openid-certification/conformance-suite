package net.openid.conformance.condition.common;

import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.ProtocolVersion;

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

	@Override
	ProtocolVersion getProtocolVersion() {
		return ProtocolVersion.TLSv12;
	}


}
