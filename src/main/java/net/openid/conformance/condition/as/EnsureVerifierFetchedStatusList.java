package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the verifier fetched the Token Status List the presented credential references,
 * which the test instance serves itself (the serving handler records the fetch in the
 * environment). A verifier that never fetched the status list cannot have checked the
 * credential's revocation status.
 *
 * <p>No specification requires the fetch itself: HAIP 5-2.6 requires verifiers to
 * <em>support</em> validating status information, and draft-ietf-oauth-status-list leaves
 * checking to the verifier's policy. The caller therefore sets the severity from what the
 * module is testing - a failure where the credential is revoked and the fetch is the only way
 * the verifier could have found out, a warning where the credential is valid.
 */
public class EnsureVerifierFetchedStatusList extends AbstractCondition {

	/** Set by the test's status list serving handler when the list is fetched. */
	public static final String FETCHED_ENV_KEY = "status_list_fetched";

	@Override
	public Environment evaluate(Environment env) {

		if (env.getString(FETCHED_ENV_KEY) == null) {
			throw error("The verifier did not fetch the Token Status List referenced by the presented credential, so it cannot have checked the credential's revocation status",
				args("status_list_uri", env.getString(AbstractCreateStatusListReference.ENV_KEY, "uri")));
		}

		logSuccess("The verifier fetched the Token Status List referenced by the presented credential");
		return env;
	}
}
