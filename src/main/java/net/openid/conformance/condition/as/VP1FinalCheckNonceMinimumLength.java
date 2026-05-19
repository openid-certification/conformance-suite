package net.openid.conformance.condition.as;

/**
 * Section 5.2 of OID4VP 1.0 Final requires the nonce to be a "fresh,
 * cryptographically random number with sufficient entropy" but does not impose
 * a minimum length. This check flags obviously-too-short nonces; values
 * shorter than 16 characters cannot plausibly carry sufficient entropy
 * regardless of alphabet. VP1FinalEnsureMinimumNonceEntropy is the substantive
 * entropy check.
 */
public class VP1FinalCheckNonceMinimumLength extends CheckNonceMinimumLength {

	@Override
	protected String buildTooShortMessage() {
		return ("Nonce is shorter than %d characters. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\" " +
			"but does not impose a minimum length. Values shorter than %d characters cannot plausibly carry sufficient entropy " +
			"regardless of alphabet, so they cannot satisfy the spec's \"sufficient entropy\" requirement.").formatted(MIN_LEN, MIN_LEN);
	}

	@Override
	protected String buildSuccessMessage() {
		return ("Nonce is at least %d characters. " +
			"Section 5.2 of OID4VP 1.0 Final does not impose a minimum length; this check flags obviously-too-short " +
			"nonces that cannot plausibly satisfy the spec's \"sufficient entropy\" requirement.").formatted(MIN_LEN);
	}
}
