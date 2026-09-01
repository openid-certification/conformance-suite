package net.openid.conformance.fapiciba;

public final class OpenBankingBrazilCibaProfileConstants {

	/**
	 * Default ceiling of 86,400 seconds (24 hours), retained from the original beta2 implementation.
	 * This is a test-suite fallback when the tester does not configure the product- or service-specific maximum,
	 * not a maximum defined by the specification.
	 * Open Finance Brasil CIBA section 6.2.6 does not define one fixed maximum for all products and services.
	 */
	public static final int DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS = 86_400;

	private OpenBankingBrazilCibaProfileConstants() {
	}
}
