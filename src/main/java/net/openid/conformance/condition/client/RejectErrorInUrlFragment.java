package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RejectErrorInUrlFragment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_params", "error"))) {
			throw error("'error' is present in URL fragment returned from authorization endpoint - it should be returned in the URL query only");
		}

		logSuccess("'error' is not present in URL fragment returned from authorization endpoint");
		return env;
	}
}
