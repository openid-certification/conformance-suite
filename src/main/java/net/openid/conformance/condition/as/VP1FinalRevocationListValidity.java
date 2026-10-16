package net.openid.conformance.condition.as;

import java.time.Duration;

/**
 * How long the revocation lists the VP1 Final verifier tests serve are valid for, whichever
 * representation or mechanism they use.
 */
public final class VP1FinalRevocationListValidity {

	/**
	 * Time from issuance to expiry. The list is minted once, when the credential is created, so
	 * this has to cover a verifier that defers its verification and only fetches the list well
	 * after the presentation.
	 */
	public static final Duration LIFETIME = Duration.ofHours(24);

	/**
	 * The ttl claim: well inside {@link #LIFETIME}, so a cached copy is never considered fresh
	 * past the token's own expiry.
	 */
	public static final Duration TTL = Duration.ofMinutes(5);

	private VP1FinalRevocationListValidity() {
	}
}
