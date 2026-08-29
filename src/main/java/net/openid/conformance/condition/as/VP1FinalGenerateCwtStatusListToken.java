package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Generates the MSO revocation list for the mdoc the emulated wallet presents, so that a
 * verifier that trusts the suite's IACA root can verify it with no further configuration.
 *
 * <p>Stores the token base64 encoded in {@code served_status_list_cwt}.
 */
public class VP1FinalGenerateCwtStatusListToken extends AbstractGenerateCwtStatusListToken {

	public static final String ENV_KEY = "served_status_list_cwt";

	@Override
	@PreEnvironment(required = { AbstractCreateStatusListReference.ENV_KEY })
	@PostEnvironment(strings = { ENV_KEY })
	public Environment evaluate(Environment env) {

		String uri = OIDFJSON.getString(
			env.getElementFromObject(AbstractCreateStatusListReference.ENV_KEY, "uri"));

		generateStatusListToken(env, ENV_KEY, uri, VP1FinalRevocationListValidity.LIFETIME,
			VP1FinalRevocationListValidity.TTL, null);

		return env;
	}
}
