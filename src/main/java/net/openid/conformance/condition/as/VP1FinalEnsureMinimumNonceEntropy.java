package net.openid.conformance.condition.as;

/**
 * Section 5.2 of OID4VP 1.0 Final requires the nonce to be a "fresh,
 * cryptographically random number with sufficient entropy". This check
 * estimates the Shannon entropy of the nonce against a 96-bit threshold
 * (slack from a 128-bit target, since Shannon entropy can only be estimated).
 */
public class VP1FinalEnsureMinimumNonceEntropy extends EnsureMinimumNonceEntropy {

	@Override
	protected String buildSuccessMessage() {
		return "Nonce appears to have sufficient entropy. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\".";
	}

	@Override
	protected String buildErrorMessage() {
		return "Nonce does not appear to have sufficient entropy (calculated Shannon entropy is below the threshold). " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\".";
	}
}
