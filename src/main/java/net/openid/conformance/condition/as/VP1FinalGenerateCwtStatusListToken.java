package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Generates the MSO revocation list for the mdoc the emulated wallet presents, so that a
 * verifier that trusts the suite's IACA root can verify it with no further configuration.
 *
 * <p>Stores the list as the {@link ServedRevocationList}.
 */
public class VP1FinalGenerateCwtStatusListToken extends AbstractGenerateCwtStatusListToken {

	@Override
	@PreEnvironment(required = { RevocationListReference.ENV_KEY })
	@PostEnvironment(required = { ServedRevocationList.ENV_KEY })
	public Environment evaluate(Environment env) {

		String uri = OIDFJSON.getString(
			env.getElementFromObject(RevocationListReference.ENV_KEY, "uri"));

		String token = generateStatusListToken(uri, VP1FinalRevocationListValidity.LIFETIME,
			VP1FinalRevocationListValidity.TTL, null);
		ServedRevocationList.store(env, "status list", StatusListCwt.CONTENT_TYPE, token, true);

		return env;
	}
}
