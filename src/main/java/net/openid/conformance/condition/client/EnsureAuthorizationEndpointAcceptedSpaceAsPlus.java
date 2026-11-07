package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthorizationEndpointAcceptedSpaceAsPlus extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("authorization_endpoint_response", "error"))) {
			throw error("The authorization server returned an error for a request in which the space in the scope"
				+ " value was encoded as '+'. RFC 6749 section 4.1.1 requires the authorization request query to use"
				+ " the application/x-www-form-urlencoded format, in which '+' is the encoding of a space that"
				+ " appendix B's own example uses, so the server must decode it as a space.",
				env.getObject("authorization_endpoint_response"));
		}
		logSuccess("The authorization server accepted the request with the space in the scope encoded as '+'");
		return env;
	}
}
