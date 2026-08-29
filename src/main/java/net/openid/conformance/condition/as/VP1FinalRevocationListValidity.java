package net.openid.conformance.condition.as;

import java.time.Duration;

/**
 * How long the revocation lists the VP1 Final verifier tests serve are valid for, whichever
 * representation or mechanism they use.
 */
public final class VP1FinalRevocationListValidity {

	/**
	 * Time from issuance to expiry: long enough that a verifier deferring its verification does
	 * not see an expired list.
	 */
	public static final Duration LIFETIME = Duration.ofHours(1);

	/**
	 * The ttl claim: well inside {@link #LIFETIME}, so a cached copy is never considered fresh
	 * past the token's own expiry.
	 */
	public static final Duration TTL = Duration.ofMinutes(5);

	private VP1FinalRevocationListValidity() {
	}
}
