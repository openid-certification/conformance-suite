package net.openid.conformance.condition.as;

/**
 * Flags verifier nonces that are obviously not random.
 *
 * <p>OID4VP 1.0 Final section 5.2 (nonce):
 * <blockquote>The Verifier MUST create a fresh, cryptographically random number with
 * sufficient entropy for every Authorization Request, store it with its current
 * session, and pass it in the nonce Authorization Request Parameter to the
 * Wallet.</blockquote>
 *
 * <p>OID4VP 1.0 Final section 13.3, step (1) of the reference design for Response
 * Mode direct_post:
 * <blockquote>The Verifier produces a nonce value by generating at least 16 fresh,
 * cryptographically random bytes with sufficient entropy, associates it with the
 * session and base64url encodes it.</blockquote>
 *
 * <p>OID4VP 1.0 Final sets no minimum number of bits for the nonce. The OpenID4VP 1.1
 * working group draft, section 5.2 (added by https://github.com/openid/OpenID4VP/pull/722),
 * adds:
 * <blockquote>The nonce SHOULD contain at least 128 bits of entropy.</blockquote>
 * HAIP 1.0 Final adds nothing on the verifier's nonce.
 *
 * <p>The suite cannot measure how a nonce was generated. The estimate used here is the
 * Shannon entropy of the nonce's character frequencies times its length, which is bounded
 * by the length: a 22-character nonce (base64url of 16 bytes, as in section 13.3) measures
 * at most 22 &times; log2(22) &asymp; 98 bits, and typically about 92 bits, although it carries
 * 128 bits. The threshold is therefore not the 128 bits a nonce should carry but the
 * largest round value that random 16-byte nonces in common encodings (22-character
 * base64url, 32-character hex, UUIDv4) do not fall to: none of 2,000,000 random
 * base64url values measured below 69 bits. A nonce at or below 64 bits has repeated
 * characters or a pattern that no 16 random bytes would plausibly produce. A 16-character
 * nonce measures at most 64 bits, so it always fails; it carries at most 96 bits even in
 * base64url, less than the 16 bytes of section 13.3.
 *
 * <p>Passing this check does not show that the nonce is random: a counter or a timestamp of
 * the same length can pass.
 */
public class VP1FinalEnsureMinimumNonceEntropy extends EnsureMinimumNonceEntropy {

	@Override
	protected double getRequiredEntropy() {
		return 64;
	}

	@Override
	protected String buildSuccessMessage() {
		return "Nonce does not look obviously non-random. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\", " +
			"and the reference design in section 13.3 generates it from \"at least 16 fresh, cryptographically random bytes\". " +
			"The check estimates entropy from the character frequencies, which cannot show that the nonce is random.";
	}

	@Override
	protected String buildErrorMessage() {
		return "Nonce does not appear to have sufficient entropy: its Shannon entropy estimate is lower than a nonce made from " +
			"16 random bytes would plausibly measure, so it is too short or has too many repeated characters. " +
			"Section 5.2 of OID4VP 1.0 Final requires the nonce to be a \"fresh, cryptographically random number with sufficient entropy\", " +
			"and the reference design in section 13.3 generates it from \"at least 16 fresh, cryptographically random bytes\".";
	}
}
