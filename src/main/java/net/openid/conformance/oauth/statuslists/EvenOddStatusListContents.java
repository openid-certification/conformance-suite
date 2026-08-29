package net.openid.conformance.oauth.statuslists;

/**
 * The contents of the Token Status List the test suite serves when it emulates a credential
 * issuer (OpenID4VCI wallet tests) or the issuer of the credential it presents (OpenID4VP
 * verifier tests). Shared by the JWT and CWT representations of the list so that both say the
 * same thing about the same index.
 *
 * <p>Every even index is VALID and every odd index is INVALID, which the credential-creating
 * conditions rely on when they allocate an index for a credential: a credential that is meant
 * to be accepted gets an even index, one that is meant to be rejected as revoked gets an odd
 * index.
 */
public final class EvenOddStatusListContents {

	/**
	 * Size of the served status list. Must stay in step with the index allocation in the
	 * conditions that reference it, which only hand out indices below this bound.
	 */
	public static final int STATUS_LIST_ENTRIES = 256;

	/** ISO/IEC 18013-5 12.3.6.5 requires the bits element to be 1 for an MSO revocation list. */
	public static final int BITS = 1;

	private EvenOddStatusListContents() {
		// utility class
	}

	public static TokenStatusList create() {
		byte[] rawEntries = new byte[STATUS_LIST_ENTRIES];
		for (int i = 0; i < rawEntries.length; i++) {
			// mark every token value with an even index as valid
			rawEntries[i] = (byte) (i % 2 == 0
				? TokenStatusList.Status.VALID.getTypeValue()
				: TokenStatusList.Status.INVALID.getTypeValue());
		}
		return TokenStatusList.create(rawEntries, BITS);
	}
}
