package net.openid.conformance.condition.common;

@SuppressWarnings("deprecation")
public class DisallowTLS10RequireBCP195Ciphers extends DisallowTLS10 {

	@Override
	protected boolean useBCP195Ciphers() {
		return true;
	}
}
