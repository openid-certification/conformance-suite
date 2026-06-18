package net.openid.conformance.condition.common;

import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.ProtocolVersion;

public class EnsureTLS13OrLater extends EnsureTLS12OrLater {

	@Override
	protected ProtocolVersion[] getAllowedProtocolVersions() {
		return new ProtocolVersion[]{ProtocolVersion.TLSv13};
	}

	@Override
	protected void negotiatedProtocolVersions(Environment env, ProtocolVersion serverVersion) {
		if (ProtocolVersion.contains(getAllowedProtocolVersions(), serverVersion)) {
			env.putString("tls13_negotiated", "");
		}
		else {
			env.removeNativeValue("tls13_negotiated");
		}
	}

}
