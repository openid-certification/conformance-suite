package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.FAPITLSClient;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsClient;
import org.bouncycastle.tls.TlsClientProtocol;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnsureTLS12OrLater extends AbstractCondition {

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

				TlsClient client = createTlsClient(tlsTestHost);

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
			if (Set.of(getAllowedProtocolVersions()).contains(serverVersion)) {
				String protocolVersionNames = Arrays.stream(getAllowedProtocolVersions()).map(ProtocolVersion::getName).collect(Collectors.joining(" or "));
				logSuccess("Server agreed to " + protocolVersionNames, args("host", tlsTestHost, "port", tlsTestPort));
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

	protected TlsClient createTlsClient(String tlsTestHost) {
		return new FAPITLSClient(tlsTestHost, useOnlyFAPICiphers(), getAllowedProtocolVersions());
	}

	protected boolean useOnlyFAPICiphers() {
		return false;
	}

	protected ProtocolVersion[] getAllowedProtocolVersions() {
		return new ProtocolVersion[]{ProtocolVersion.TLSv12, ProtocolVersion.TLSv13};
	}

}
