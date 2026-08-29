package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the verifier fetched the identifier list (ISO/IEC 18013-5 12.3.6.4) the presented
 * mdoc's MSO references, which the test instance serves itself (the serving handler records the
 * fetch in the environment). A verifier that never fetched the identifier list cannot have
 * checked the MSO's revocation status.
 *
 * <p>The identifier list counterpart of {@link EnsureVerifierFetchedStatusList}; the caller is
 * expected to pick the severity the same way.
 */
public class EnsureVerifierFetchedIdentifierList extends AbstractCondition {

	/** Set by the test's identifier list serving handler when the list is fetched. */
	public static final String FETCHED_ENV_KEY = "identifier_list_fetched";

	@Override
	public Environment evaluate(Environment env) {

		if (env.getString(FETCHED_ENV_KEY) == null) {
			throw error("The verifier did not fetch the identifier list referenced by the presented credential's Mobile Security Object, so it cannot have checked the credential's revocation status",
				args("identifier_list_uri", env.getString(CreateRevokedIdentifierListReference.ENV_KEY, "uri")));
		}

		logSuccess("The verifier fetched the identifier list referenced by the presented credential's Mobile Security Object");
		return env;
	}
}
