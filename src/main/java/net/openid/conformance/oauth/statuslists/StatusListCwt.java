package net.openid.conformance.oauth.statuslists;

/**
 * The wire constants of a Status List Token in CWT format
 * (draft-ietf-oauth-status-list section 5.2), shared by the suite's writer
 * ({@link CwtStatusListTokenBuilder}) and its readers (the conditions that consume an MSO
 * revocation list) so the two cannot disagree about a claim key or the media type.
 */
public final class StatusListCwt {

	/** Media type of a Status List Token in CWT format. */
	public static final String CONTENT_TYPE = "application/statuslist+cwt";

	public static final long CLAIM_SUB = 2;
	public static final long CLAIM_EXP = 4;
	public static final long CLAIM_IAT = 6;
	public static final long CLAIM_STATUS_LIST = 65533;
	public static final long CLAIM_TTL = 65534;

	private StatusListCwt() {
		// constants holder
	}
}
