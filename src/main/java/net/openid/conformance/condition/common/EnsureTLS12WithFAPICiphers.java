package net.openid.conformance.condition.common;

public class EnsureTLS12WithFAPICiphers extends EnsureTLS12OrLater {

	@Override
	protected boolean useOnlyFAPICiphers() {
		return true;
	}
}
