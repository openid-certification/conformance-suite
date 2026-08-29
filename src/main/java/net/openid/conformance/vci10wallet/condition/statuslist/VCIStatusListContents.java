package net.openid.conformance.vci10wallet.condition.statuslist;

import net.openid.conformance.oauth.statuslists.TokenStatusList;

/**
 * The contents of the Token Status List the emulated credential issuer serves, shared by the
 * JWT and CWT representations of it so both say the same thing about the same index.
 *
 * <p>Every even index is VALID and every odd index is INVALID, which the credential-issuing
 * conditions rely on when they allocate an index for a credential.
 */
public final class VCIStatusListContents {

	/**
	 * Size of the served status list. Must stay in step with the index allocation in
	 * {@code CreateSdJwtCredential} and {@code CreateMdocCredentialForVCI}, which only hand out
	 * even indices.
	 */
	public static final int STATUS_LIST_ENTRIES = 256;

	/** ISO/IEC 18013-5 12.3.6.5 requires the bits element to be 1 for an MSO revocation list. */
	public static final int BITS = 1;

	private VCIStatusListContents() {
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
