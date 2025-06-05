package net.openid.conformance.condition.common;

@SuppressWarnings("deprecation")
public class DisallowTLS11RequireBCP195Ciphers extends DisallowTLS11 {

	@Override
	protected boolean useBCP195Ciphers() {
		return true;
	}
}
