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


@SuppressWarnings("deprecation")
public abstract class AbstractCheckInsecureCiphers extends AbstractCondition {

	abstract Map<Integer, String> getInsecureCiphers();

	@Override
	@PreEnvironment(required = "tls")
	public Environment evaluate(Environment env) {

		Map<Integer, String>  insecureCiphers = this.getInsecureCiphers();

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
		for (var cipherEntry : insecureCiphers.entrySet()) {
			try (Socket socket = setupSocket(tlsTestHost, tlsTestPort)) {

				TlsCrypto crypto = new BcTlsCrypto(new SecureRandom());
				TlsClient client = new DefaultTlsClient(crypto) {

					@Override
					public TlsAuthentication getAuthentication() {
						return new NoopTlsAuthentication();
					}

					@Override
					public int[] getCipherSuites() {
						return new int[]{cipherEntry.getKey()};
					}
				};

				TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream());

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
			logSuccess("The TLS peer uses none of the insecure ciphers.", args("host", tlsTestHost, "port", tlsTestPort, "insecureCiphers", insecureCiphers.values()));
		} else {
			throw error("The TLS peer uses some insecure ciphers according to BCP195. The used ciphers are vulnerable to a denial of service attack as per \"RFC9325 Appendix A. Differences from RFC 7525\"", args("host", tlsTestHost, "port", tlsTestPort, "insecureCiphers", detectedUnwantedCypherSuites));
		}

		return env;
	}
}
