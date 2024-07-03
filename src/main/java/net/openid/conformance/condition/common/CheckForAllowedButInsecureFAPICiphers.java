package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.NoopTlsAuthentication;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClient;
import org.bouncycastle.crypto.tls.TlsClientProtocol;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Some cipher suites that are officially allowed by FAPI are considered insecure.
 * See <a href="https://bitbucket.org/openid/fapi/issues/685/use-of-tls-12-ciphers#comment-66826146">Vulnerability in TLS_DHE_RSA_WITH_AES_128_GCM_SHA256</a>
 */
@SuppressWarnings("deprecation")
public class CheckForAllowedButInsecureFAPICiphers extends AbstractCondition {

	/**
	 * This map contains the cipher suites  which should produce a warning when detected.
	 */
	private static final Map<Integer, String> ALLOWED_BUT_INSECURE_CIPHERS = Map.ofEntries(
		Map.entry(CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, "DHE_RSA_WITH_AES_128_GCM_SHA256"),
		Map.entry(CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, "DHE_RSA_WITH_AES_256_GCM_SHA384")
	);

	@Override
	@PreEnvironment(required = "tls")
	public Environment evaluate(Environment env) {

		String tlsTestHost = env.getString("tls", "testHost");
		Integer tlsTestPort = env.getInteger("tls", "testPort");

		if (Strings.isNullOrEmpty(tlsTestHost)) {
			throw error("Couldn't find host to connect for TLS");
		}

		if (tlsTestPort == null) {
			throw error("Couldn't find port to connect for TLS");
		}

		List<String> detectedUnwantedCypherSuites = new ArrayList<>();

		// Unfortunately there is no easy way to query the server for supported cipher suites.
		// Therefore we attempt to connect with an insecure cipher - if it succeeds the server supports the cipher
		// and we can generate a warning.
		for (var cipherEntry : ALLOWED_BUT_INSECURE_CIPHERS.entrySet()) {
			try (Socket socket = setupSocket(tlsTestHost, tlsTestPort)) {

				TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream(), new SecureRandom());
				TlsClient client = new DefaultTlsClient() {

					@Override
					public TlsAuthentication getAuthentication() {
						return new NoopTlsAuthentication();
					}

					@Override
					public int[] getCipherSuites() {
						return new int[]{cipherEntry.getKey()};
					}
				};

				protocol.connect(client);

				// connection succeeded, add this cipher suite candidate to the list
				detectedUnwantedCypherSuites.add(cipherEntry.getValue());

			} catch (IOException e) {
				if (e.getCause() instanceof ConditionError) {
					// It's our own error; pass it on
					throw (ConditionError) e.getCause();
				}
			}
		}

		if (detectedUnwantedCypherSuites.isEmpty()) {
			logSuccess("The TLS peer uses none of the allowed but insecure ciphers.", args("host", tlsTestHost, "port", tlsTestPort, "insecureCiphers", ALLOWED_BUT_INSECURE_CIPHERS.values()));
		} else {
			logFailure("The TLS peer uses some allowed but insecure ciphers.", args("host", tlsTestHost, "port", tlsTestPort, "insecureCiphers", detectedUnwantedCypherSuites));
		}

		return env;
	}
}
