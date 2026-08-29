package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the verifier fetched the Token Status List the presented credential references,
 * which the test instance serves itself (the serving handler records the fetch in the
 * environment). A verifier that never fetched the status list cannot have checked the
 * credential's revocation status.
 *
 * <p>HAIP requires verifiers to support validating the status information of a credential
 * (HAIP 7-2.2.2.2); note the requirement is on <em>supporting</em> the validation, so callers
 * outside HAIP are expected to treat a missing fetch as a warning rather than a failure.
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
