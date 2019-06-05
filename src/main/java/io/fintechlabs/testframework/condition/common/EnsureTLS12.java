package io.fintechlabs.testframework.condition.common;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Vector;

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

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureTLS12 extends AbstractCondition {

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
								// even though we make a TLS connection we ignore the server cert validation here
							}
						};
					}

					@Override
					public ProtocolVersion getMinimumVersion() {
						// Disallow anything earlier than TLS 1.2
						return ProtocolVersion.TLSv12;
					}

					@Override
					public ProtocolVersion getClientVersion() {
						// Try to connect with TLS 1.2
						return ProtocolVersion.TLSv12;
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
			if (serverVersion == ProtocolVersion.TLSv12) {
				logSuccess("Server agreed to TLS 1.2", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else {
				throw error("Server used incorrect TLS version",
					args("server_version", serverVersion.toString(),
						"host", tlsTestHost,
						"port", tlsTestPort));
			}
		} catch (IOException e) {
			throw error("Failed to make TLS connection", e, args("host", tlsTestHost, "port", tlsTestPort));
		}

	}

}
