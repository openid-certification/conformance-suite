package net.openid.conformance.condition.common;


import org.bouncycastle.tls.ProtocolVersion;

@SuppressWarnings("deprecation")
public class DisallowTLS10 extends AbstractDisallowTLS {

	@Override
	ProtocolVersion getDisallowedProtocol() {
		return ProtocolVersion.TLSv10;
	}

}
