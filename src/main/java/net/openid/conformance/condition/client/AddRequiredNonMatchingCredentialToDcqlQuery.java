package net.openid.conformance.condition.client;

/**
 * Adds a second credential entry to the DCQL query that will never match any
 * real credential (uses an impossible vct value), and wraps both in credential_sets
 * where both are required.
 *
 * As the wallet cannot satisfy the required non-matching credential, the DCQL query as a
 * whole cannot be satisfied, so the wallet must return an error response rather than a
 * VP Token — in particular it must not return a partial vp_token containing only the
 * matchable credential, nor an empty vp_token object.
 *
 * The DCQL query must NOT already contain credential_sets.
 */
public class AddRequiredNonMatchingCredentialToDcqlQuery extends AbstractAddNonMatchingCredentialToDcqlQuery {

	@Override
	protected boolean nonMatchingCredentialRequired() {
		return true;
	}
}
