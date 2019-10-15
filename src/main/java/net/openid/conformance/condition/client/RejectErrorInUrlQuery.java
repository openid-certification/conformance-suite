package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RejectErrorInUrlQuery extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_query_params", "error"))) {
			throw error("'error' is present in URL query returned from authorization endpoint - it should be returned in the URL fragment only");
		}

		logSuccess("'error' is not present in URL query returned from authorization endpoint");
		return env;
	}
}
