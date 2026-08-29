package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the verifier fetched the revocation list the presented credential references -
 * its Token Status List or, for an mdoc using that mechanism, its identifier list - which the
 * test instance serves itself (the serving handler records the fetch in the environment). A
 * verifier that never fetched the list cannot have checked the credential's revocation status.
 *
 * <p>No specification makes the fetch mandatory: draft-ietf-oauth-sd-jwt-vc section 3.4 says
 * the status SHOULD be checked when the claim is present, ISO/IEC 18013-5 12.3.6.1 makes
 * verifying the MSO revocation list optional for the mdoc reader, and HAIP section 7 only fixes
 * the algorithm a verifier must support for validating status information, which places no
 * duty on it to validate. The caller therefore sets the severity from what the module is
 * testing - a failure where the credential is revoked and the fetch is the only way the
 * verifier could have found out, a warning where the credential is valid.
 */
public class EnsureVerifierFetchedRevocationList extends AbstractCondition {

	/** Set by the test's revocation list serving handler when the list is fetched. */
	public static final String FETCHED_ENV_KEY = "revocation_list_fetched";

	@Override
	@PreEnvironment(required = RevocationListReference.ENV_KEY)
	public Environment evaluate(Environment env) {

		String uri = env.getString(RevocationListReference.ENV_KEY, "uri");

		if (env.getString(FETCHED_ENV_KEY) == null) {
			throw error("The verifier did not fetch the revocation list referenced by the presented credential, so it cannot have checked the credential's revocation status",
				args("revocation_list_uri", uri));
		}

		logSuccess("The verifier fetched the revocation list referenced by the presented credential",
			args("revocation_list_uri", uri));
		return env;
	}
}
