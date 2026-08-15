package net.openid.conformance.statistics;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the {@code iss} and {@code sub} of a test plan's owner with a small integer
 * while the cube is being built.
 *
 * <p>The only thing the statistics ever do with a user is count distinct ones, so the
 * identifiers themselves - which name real people - are not worth keeping in a snapshot
 * that lives in memory for twelve hours and is walked on every request. Ids are unique
 * within one cube and mean nothing outside it.
 *
 * <p>Not thread safe: one instance belongs to one cube computation.
 */
final class OwnerIds {

	/** Separates the length prefix from the identifiers it makes unambiguous. */
	private static final String SEPARATOR = ":";

	private final Map<String, Integer> ids = new HashMap<>();

	private int next;

	/**
	 * @param iss the owner's issuer
	 * @param sub the owner's subject
	 * @return true if this is a real identity. A document written before authentication
	 *         completed, or one that lost its owner, is not a user and must not be counted
	 *         as one.
	 */
	static boolean isUser(String iss, String sub) {
		return iss != null && !iss.isBlank() && sub != null && !sub.isBlank();
	}

	/**
	 * @param iss the owner's issuer
	 * @param sub the owner's subject
	 * @return the id of that owner, the same one every time it is asked for
	 */
	int idFor(String iss, String sub) {
		return ids.computeIfAbsent(compositeKey(iss, sub), key -> next++);
	}

	/**
	 * @return one string standing for the pair. The length of the {@code iss} is written
	 *         out in front of it rather than a separator being put between the two, so that
	 *         two different pairs cannot produce the same key whatever characters they
	 *         contain - there is nothing an identity provider is forbidden from putting in
	 *         an {@code iss} that this would have to rely on.
	 */
	private static String compositeKey(String iss, String sub) {
		return iss.length() + SEPARATOR + iss + sub;
	}
}
