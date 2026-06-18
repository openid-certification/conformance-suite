package net.openid.conformance.condition.common;

import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.ProtocolVersion;

// We know the server supports tls 1.3. Ensure it negotiates it over tls 1.2
public class EnsureTLS13PreferredOverTLS12 extends EnsureTLS12OrLater {

	private ProtocolVersion serverVersion;

	@Override
	protected ProtocolVersion[] getAllowedProtocolVersions() {
		return new ProtocolVersion[]{ProtocolVersion.TLSv13, ProtocolVersion.TLSv12};
	}

	@Override
	protected void negotiatedProtocolVersions(Environment env, ProtocolVersion serverVersion) {
		// Tidy up string set by EnsureTLS13OrLater that indicates tls 1.3 is supported.
		env.removeNativeValue("tls13_negotiated");

		if (! serverVersion.equals(ProtocolVersion.TLSv13)) {
			throw error("Server negotiated TLS 1.2 when TLS 1.3 was available");
		}

		this.serverVersion = serverVersion;
	}

	@Override
	protected void logSuccessMessage(String tlsTestHost, Integer tlsTestPort) {
		logSuccess("Server agreed to " + serverVersion.getName(), args("host", tlsTestHost, "port", tlsTestPort));
	}

}
