package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNoPostLogoutState extends AbstractCondition {

	@Override
	@PreEnvironment(required = "post_logout_redirect")
	public Environment evaluate(Environment env) {
		String state = env.getString("post_logout_redirect", "query_string_params.state");
		if (state != null) {
			throw error("state present in query params passed to post logout redirect uri, but no state was passed to request.");
		}

		logSuccess("state not passed to post logout redirect uri.");

		return env;

	}

}
