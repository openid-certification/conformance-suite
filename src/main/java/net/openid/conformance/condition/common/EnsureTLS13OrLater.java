package net.openid.conformance.condition.common;

import org.bouncycastle.tls.ProtocolVersion;

public class EnsureTLS13OrLater extends EnsureTLS12OrLater {

	@Override
	protected ProtocolVersion[] getAllowedProtocolVersions() {
		return new ProtocolVersion[]{ProtocolVersion.TLSv13};
	}

}
