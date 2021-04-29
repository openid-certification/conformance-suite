package net.openid.conformance.condition.as.par;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientIdMatchesForPAREndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "authorization_request_object"})
	public Environment evaluate(Environment env) {

		// This will be called after authentication so the client_id should match
		String expected = env.getString("client", "client_id");
		String actual = env.getString("authorization_request_object", "claims.client_id");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Client ID in request object matched the authenticated client id", args("client_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between authenticated client ID and client id in the request object", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
