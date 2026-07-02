package net.openid.conformance.condition.client;

/**
 * Adds a second credential entry to the DCQL query that will never match any
 * real credential (uses an impossible vct value), and wraps both in credential_sets
 * where the original is required and the fake one is optional.
 *
 * This tests that wallets correctly handle credential_sets with optional entries
 * and don't fail the whole request when an optional credential cannot be matched.
 *
 * The DCQL query must NOT already contain credential_sets.
 */
public class AddOptionalNonMatchingCredentialToDcqlQuery extends AbstractAddNonMatchingCredentialToDcqlQuery {

	@Override
	protected boolean nonMatchingCredentialRequired() {
		return false;
	}
}
