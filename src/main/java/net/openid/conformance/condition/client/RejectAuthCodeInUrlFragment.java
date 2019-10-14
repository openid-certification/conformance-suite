package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RejectAuthCodeInUrlFragment extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_params", "code"))) {
			throw error("Authorization code is present in URL fragment returned from authorization endpoint");
		}

		logSuccess("Authorization code is not present in URL fragment returned from authorization endpoint");
		return env;
	}

}
