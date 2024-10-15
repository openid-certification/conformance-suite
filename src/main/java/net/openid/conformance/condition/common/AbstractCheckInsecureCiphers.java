package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.crypto.tls.AlertDescription;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.NameType;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.ServerName;
import org.bouncycastle.crypto.tls.ServerNameList;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClient;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.bouncycastle.crypto.tls.TlsExtensionsUtils;
import org.bouncycastle.crypto.tls.TlsFatalAlert;
import org.bouncycastle.crypto.tls.TlsFatalAlertReceived;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

@SuppressWarnings("deprecation")
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

				TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream(), new SecureRandom());

				TlsClient client = new DefaultTlsClient() {

					@Override
					public TlsAuthentication getAuthentication() {
						return new TlsAuthentication() {

							@Override
							public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
								return null;
							}

							@Override
							public void notifyServerCertificate(Certificate serverCertificate) throws IOException {
								// Don't care
							}
						};
					}

					@Override
					public int[] getCipherSuites() {
						return getInsecureCiphers().keySet().stream().mapToInt(Integer::intValue).toArray();
					}

					@Override
					public ProtocolVersion getMinimumVersion() {
						return getProtocolVersion();
					}

					@Override
					@SuppressWarnings({"rawtypes", "JdkObsolete"}) // fit with the API
					public Hashtable getClientExtensions() throws IOException {
						Hashtable clientExtensions = super.getClientExtensions();
						Vector<ServerName> serverNameList = new Vector<>();
						serverNameList.addElement(new ServerName(NameType.host_name, tlsTestHost));
						TlsExtensionsUtils.addServerNameExtension(clientExtensions, new ServerNameList(serverNameList));
						return clientExtensions;
					}

					@Override
					public void notifySelectedCipherSuite(int selectedCipherSuite) {
						Map<Integer, String> insecureCiphers = getInsecureCiphers();
						throw error("Server accepted a cipher that is not on the list of FAPI-RW permitted ciphers",
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
