package net.openid.conformance.condition.common;

import org.bouncycastle.tls.ProtocolVersion;

@SuppressWarnings("deprecation")
public class DisallowTLS10 extends AbstractDisallowTLSVersion {

	@Override
	ProtocolVersion getDisallowedProtocol() {
		return ProtocolVersion.TLSv10;
	}

	@Override
	String getProtocolVersion() {
		return "TLS 1.0";
	}

}
