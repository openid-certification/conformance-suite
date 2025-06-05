package net.openid.conformance.condition.common;

public class EnsureTLS12RequireBCP195Ciphers extends EnsureTLS12OrLater {

	@Override
	protected boolean useOnlyFAPICiphers() {
		return true;
	}

	@Override
	protected boolean useBCP195Ciphers() {
		return true;
	}
}
