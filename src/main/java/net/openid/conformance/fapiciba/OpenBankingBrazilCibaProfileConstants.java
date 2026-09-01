package net.openid.conformance.fapiciba;

public final class OpenBankingBrazilCibaProfileConstants {

	/**
	 * Default ceiling used when the tester does not configure the product- or service-specific maximum.
	 * Open Finance Brasil CIBA section 6.2.6 does not define one fixed maximum for all products and services.
	 */
	public static final int DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS = 86_400;

	private OpenBankingBrazilCibaProfileConstants() {
	}
}
