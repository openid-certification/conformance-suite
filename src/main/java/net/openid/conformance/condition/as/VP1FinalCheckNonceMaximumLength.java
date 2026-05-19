package net.openid.conformance.condition.as;

/**
 * Section 5.2 of OID4VP 1.0 Final requires the nonce to be a "fresh,
 * cryptographically random number with sufficient entropy" but does not bound
 * its length. This check exists to promote interoperability: 43 characters is
 * the longest nonce the conformance suite generates as a verifier in the
 * corresponding wallet tests; longer values may not be accepted by all wallets.
 */
public class VP1FinalCheckNonceMaximumLength extends CheckNonceMaximumLength {

	@Override
	protected String buildOverlongMessage() {
		return ("Nonce contains in excess of %d characters. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\" " +
			"but does not specify its length. To promote interoperability we expect nonces no longer than %d characters " +
			"(the longest the conformance suite itself generates as a verifier when testing a wallet); " +
			"longer values may not be accepted by all wallets.").formatted(MAX_LEN, MAX_LEN);
	}

	@Override
	protected String buildSuccessMessage() {
		return ("Nonce does not exceed %d characters. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\" " +
			"but does not specify its length. To promote interoperability we expect nonces no longer than %d characters " +
			"(the longest the conformance suite itself generates as a verifier when testing a wallet); " +
			"longer values may not be accepted by all wallets.").formatted(MAX_LEN, MAX_LEN);
	}
}
