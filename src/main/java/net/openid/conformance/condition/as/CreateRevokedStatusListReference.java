package net.openid.conformance.condition.as;

import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;

import java.security.SecureRandom;

/**
 * Allocates a status list reference at an index the served status list marks as revoked - the
 * odd indices of {@link EvenOddStatusListContents}.
 */
public class CreateRevokedStatusListReference extends AbstractCreateStatusListReference {

	@Override
	protected int allocateIndex(SecureRandom random) {
		// odd indices are the revoked ones; chosen at random rather than fixed so that two runs
		// of the test do not present credentials that a verifier could correlate by index
		return 1 + 2 * random.nextInt(EvenOddStatusListContents.STATUS_LIST_ENTRIES / 2);
	}

	@Override
	protected String successMessage() {
		return "Allocated a status list index that the served status list marks as revoked";
	}
}
