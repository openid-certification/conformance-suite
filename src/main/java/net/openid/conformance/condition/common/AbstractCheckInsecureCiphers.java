package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.NoopTlsAuthentication;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsClient;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsFatalAlertReceived;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public abstract class AbstractCheckInsecureCiphers extends AbstractCondition {

	abstract Map<Integer, String> getInsecureCiphers();
	abstract ProtocolVersion getProtocolVersion();

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

		try {
			Socket socket = setupSocket(tlsTestHost, tlsTestPort);

			try {

				TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream());

				TlsCrypto crypto = new BcTlsCrypto(new SecureRandom());
				TlsClient client = new DefaultTlsClient(crypto) {

					@Override
					public TlsAuthentication getAuthentication() {
						return new NoopTlsAuthentication();
					}

					@Override
					public int[] getCipherSuites() {
						return getInsecureCiphers().keySet().stream().mapToInt(Integer::intValue).toArray();
					}

					@Override
					protected ProtocolVersion[] getSupportedVersions() {
						return new ProtocolVersion[]{getProtocolVersion()};
					}

					@Override
					protected Vector<ServerName> getSNIServerNames() {
						return new Vector<ServerName>(List.of(new ServerName(NameType.host_name, tlsTestHost.getBytes(StandardCharsets.UTF_8))));
					}

					@Override
					public void notifySelectedCipherSuite(int selectedCipherSuite) {
						Map<Integer, String> insecureCiphers = getInsecureCiphers();
						throw error("Server accepted a cipher that is not on the list of permitted ciphers",
								args("host", tlsTestHost,
										"port", tlsTestPort,
										"cipher_suite", insecureCiphers.get(selectedCipherSuite)));
					}
				};

				log("Trying to connect with a non-permitted cipher (this is not exhaustive: check the server configuration manually to verify conformance)",
						args("host", tlsTestHost, "port", tlsTestPort));

				protocol.connect(client);

				// By the time handshake completes an error should have been thrown, but just in case:
				throw error("Connection completed unexpectedly");

			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// Don't care
				}
			}
		} catch (IOException e) {
			if (e.getCause() instanceof ConditionError) {
				// It's our own error; pass it on
				throw (ConditionError) e.getCause();
			} else if ((e instanceof TlsFatalAlertReceived received)
					&& received.getAlertDescription() == AlertDescription.handshake_failure) {
				logSuccess("The TLS handshake was rejected when trying to connect with disallowed ciphers.", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else if ((e instanceof TlsFatalAlert alert)
					&& alert.getAlertDescription() == AlertDescription.handshake_failure) {
				logSuccess("The TLS handshake failed when trying to connect with disallowed ciphers.", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else if ((e instanceof SocketException exception)
					&& exception.getMessage().equals("Connection reset")) {
				logSuccess("The TCP connection was reset when trying to connect with disallowed ciphers.", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else {
				throw error("Failed to make TLS connection, but in a different way than expected", e, args("host", tlsTestHost, "port", tlsTestPort));
			}
		}
	}
}
