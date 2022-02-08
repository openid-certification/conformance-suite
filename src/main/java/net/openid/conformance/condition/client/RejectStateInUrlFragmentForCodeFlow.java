package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RejectStateInUrlFragmentForCodeFlow extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_params", "code"))) {
			throw error("state is present in URL fragment/hash returned from authorization endpoint - authorization code flow requires it to be returned in the URL query only");
		}

		logSuccess("state is correctly not present in URL fragment/hash returned from authorization endpoint (as in the authorization code flow it must be returned in the URL query only)");
		return env;
	}

}
