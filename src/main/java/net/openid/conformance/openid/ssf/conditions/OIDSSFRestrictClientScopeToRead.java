package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;

/**
 * Narrows the scope the suite requests from the authorization server to {@code ssf.read}, so a
 * following stream-management call is made with a token that is deliberately insufficient:
 * the CAEP Interop Profile (2.7.3) allows {@code ssf.read} only for Read Stream Configuration
 * and Get Stream Status, and (2.7.2) requires the transmitter to reject anything beyond that
 * with an RFC 6750 3.1 error.
 */
public class OIDSSFRestrictClientScopeToRead extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String previousScope = env.getString("client", "scope");
		env.putString("ssf", "previous_client_scope", previousScope == null ? "" : previousScope);
		env.putString("client", "scope", SsfConstants.SCOPE_SSF_READ);

		logSuccess("Requesting a read-only access token", args("scope", SsfConstants.SCOPE_SSF_READ, "previous_scope", previousScope));

		return env;
	}

	public static void undo(Environment env) {
		String previousScope = env.getString("ssf", "previous_client_scope");
		if (previousScope != null && !previousScope.isEmpty()) {
			env.putString("client", "scope", previousScope);
		} else {
			// the client had no configured scope before the restriction - remove the
			// restricted value instead of leaving 'ssf.read' behind
			env.removeElement("client", "scope");
		}
		env.removeElement("ssf", "previous_client_scope");
	}
}
