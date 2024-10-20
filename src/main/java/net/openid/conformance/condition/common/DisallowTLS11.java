package net.openid.conformance.condition.common;

import org.bouncycastle.tls.ProtocolVersion;

@SuppressWarnings("deprecation")
public class DisallowTLS11 extends AbstractDisallowTLSVersion {

	@Override
	ProtocolVersion getDisallowedProtocol() {
		return ProtocolVersion.TLSv11;
	}

	@Override
	String getProtocolVersion() {
		return "TLS 1.1";
	}
}
