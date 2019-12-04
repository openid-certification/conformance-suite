package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RejectStateInUrlQueryForHybridFlow extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_query_params")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_query_params", "code"))) {
			throw error("state is present in URL query returned from authorization endpoint - hybrid/implicit flow require it to be returned in the URL fragment/hash only");
		}

		logSuccess("state is correctly not present in URL query returned from authorization endpoint (as in the hybrid flow it must be returned in the URL fragment/hash only)");
		return env;
	}

}
