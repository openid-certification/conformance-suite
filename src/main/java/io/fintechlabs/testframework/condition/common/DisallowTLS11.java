package io.fintechlabs.testframework.condition.common;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Vector;

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

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class DisallowTLS11 extends AbstractCondition {

	// Signals that the connection was aborted after discovering the server version
	@SuppressWarnings("serial")
	private static class ServerHelloReceived extends IOException {

		private ProtocolVersion serverVersion;

		public ServerHelloReceived(ProtocolVersion serverVersion) {
			this.serverVersion = serverVersion;
		}

		public ProtocolVersion getServerVersion() {
			return serverVersion;
		}

	}

	/**
	 * @param testId
	 * @param log
	 */
	public DisallowTLS11(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
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
								// even though we make a TLS connection we ignore the server cert validation here
							}
						};
					}

					@Override
					public ProtocolVersion getMinimumVersion() {
						// Disallow anything earlier than TLS 1.1
						return ProtocolVersion.TLSv11;
					}

					@Override
					public ProtocolVersion getClientVersion() {
						// Try to connect with TLS 1.1
						return ProtocolVersion.TLSv11;
					}

					@Override
					@SuppressWarnings("rawtypes") // fit with the API
					public Hashtable getClientExtensions() throws IOException {
						Hashtable clientExtensions = super.getClientExtensions();
						Vector<ServerName> serverNameList = new Vector<>();
						serverNameList.addElement(new ServerName(NameType.host_name, tlsTestHost));
						TlsExtensionsUtils.addServerNameExtension(clientExtensions, new ServerNameList(serverNameList));
						return clientExtensions;
					}

					@Override
					public void notifyServerVersion(ProtocolVersion serverVersion) throws IOException {
						// don't need to proceed further
						throw new ServerHelloReceived(serverVersion);
					}
				};

				protocol.connect(client);

				// By the time handshake completes an exception should have been thrown, but just in case:
				throw error("Connection completed unexpectedly");

			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// Don't care
				}
			}
		} catch (ServerHelloReceived e) {
			ProtocolVersion serverVersion = e.getServerVersion();
			if (serverVersion == ProtocolVersion.TLSv11) {
				throw error("The server accepted a TLS 1.1 connection. This is not permitted by the specification.", args("host", tlsTestHost, "port", tlsTestPort));
			} else {
				throw error("Server used different TLS version than requested",
					args("server_version", serverVersion.toString(),
						"host", tlsTestHost,
						"port", tlsTestPort));
			}
		} catch (IOException e) {
			if ((e instanceof TlsFatalAlertReceived)
				&& (((TlsFatalAlertReceived) e).getAlertDescription() == AlertDescription.handshake_failure ||
				((TlsFatalAlertReceived) e).getAlertDescription() == AlertDescription.protocol_version)){
				// If we get here then we haven't received a server hello agreeing on a version
				logSuccess("Server refused TLS 1.1 handshake", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else if ((e instanceof TlsFatalAlert)
				&& ((TlsFatalAlert) e).getAlertDescription() == AlertDescription.handshake_failure) {
				// If we get here then we haven't received a server hello agreeing on a version
				logSuccess("Server refused TLS 1.0 handshake", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else if ((e instanceof SocketException)
				&& ((SocketException) e).getMessage().equals("Connection reset")) {
				// AWS ELB seem to reject like this instead of by failing the handshake
				logSuccess("Server refused TLS 1.0 handshake", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else {
				throw error("Failed to make TLS connection, but in a different way than expected", e, args("host", tlsTestHost, "port", tlsTestPort));
			}
		}

	}

}
