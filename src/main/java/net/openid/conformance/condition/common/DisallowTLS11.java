package net.openid.conformance.condition.common;

import org.bouncycastle.tls.ProtocolVersion;

public class DisallowTLS11 extends AbstractDisallowTLS {

	@Override
	ProtocolVersion getDisallowedProtocol() {
		return ProtocolVersion.TLSv11;
	}
}
