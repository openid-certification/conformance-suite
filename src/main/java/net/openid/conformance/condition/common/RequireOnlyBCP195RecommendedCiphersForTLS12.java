package net.openid.conformance.condition.common;

@SuppressWarnings("deprecation")
public class RequireOnlyBCP195RecommendedCiphersForTLS12 extends DisallowInsecureCipher {

	@Override
	protected boolean useBCP195Ciphers() {
		return true;
	}
}
