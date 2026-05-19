package net.openid.conformance.condition.as;

/**
 * Section 5.2 of OID4VP 1.0 Final requires the nonce to be a "fresh,
 * cryptographically random number with sufficient entropy". OpenID4VP PR #722
 * (https://github.com/openid/OpenID4VP/pull/722/changes) recommends at least
 * 128 bits of entropy.
 *
 * Shannon entropy is only an approximation of randomness — it measures the
 * dispersion of character frequencies in the string, not true unpredictability —
 * so a string generated from a 128-bit random value will not always measure
 * exactly 128 bits of Shannon entropy. We require >96 bits to allow for that
 * approximation, avoiding false-positive failures on legitimate 128-bit nonces.
 */
public class VP1FinalEnsureMinimumNonceEntropy extends EnsureMinimumNonceEntropy {

	@Override
	protected String buildSuccessMessage() {
		return "Nonce appears to have sufficient entropy. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\"; " +
			"OpenID4VP PR #722 (https://github.com/openid/OpenID4VP/pull/722/changes) recommends at least 128 bits.";
	}

	@Override
	protected String buildErrorMessage() {
		return "Nonce does not appear to have sufficient entropy (calculated Shannon entropy is below the threshold). " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\"; " +
			"OpenID4VP PR #722 (https://github.com/openid/OpenID4VP/pull/722/changes) recommends at least 128 bits.";
	}
}
