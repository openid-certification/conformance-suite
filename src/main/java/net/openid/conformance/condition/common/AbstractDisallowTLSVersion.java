package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.FAPITLSClient;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsClient;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsFatalAlertReceived;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public abstract class AbstractDisallowTLSVersion extends AbstractCondition {

	abstract ProtocolVersion getDisallowedProtocol();
	abstract String getProtocolVersion();


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

				TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream());

				TlsClient client = new FAPITLSClient(tlsTestHost, false, getDisallowedProtocol());

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
		} catch (FAPITLSClient.ServerHelloReceived e) {
			ProtocolVersion serverVersion = e.getServerVersion();
			if (serverVersion.equals(getDisallowedProtocol())) {
				throw error("The server accepted a "+ getProtocolVersion() + " connection. This is not permitted by the specification.", args("host", tlsTestHost, "port", tlsTestPort));
			} else {
				throw error("Server used different TLS version than requested",
						args("server_version", serverVersion.toString(),
								"host", tlsTestHost,
								"port", tlsTestPort));
			}
		} catch (IOException e) {
			if ((e instanceof TlsFatalAlertReceived received)
					&& (received.getAlertDescription() == AlertDescription.handshake_failure ||
					received.getAlertDescription() == AlertDescription.protocol_version)){
				// If we get here then we haven't received a server hello agreeing on a version
				logSuccess("Server refused "+ getProtocolVersion() + " handshake", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else if ((e instanceof TlsFatalAlert alert)
					&& alert.getAlertDescription() == AlertDescription.handshake_failure) {
				// If we get here then we haven't received a server hello agreeing on a version
				logSuccess("Server refused "+ getProtocolVersion() + " handshake", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else if ((e instanceof SocketException exception)
					&& exception.getMessage().equals("Connection reset")) {
				// AWS ELB seem to reject like this instead of by failing the handshake
				logSuccess("Server refused "+ getProtocolVersion() + " handshake", args("host", tlsTestHost, "port", tlsTestPort));
				return env;
			} else {
				throw error("Failed to make TLS connection, but in a different way than expected", e, args("host", tlsTestHost, "port", tlsTestPort));
			}
		}

	}
}
