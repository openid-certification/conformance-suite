package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMaxAgeEqualsZeroAndPromptNone extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {

		String maxAge = env.getString("authorization_endpoint_request", "query_string_params.max_age");
		String prompt = env.getString("authorization_endpoint_request", "query_string_params.prompt");

		if ("0".equals(maxAge) && "none".equals(prompt)) {
			logSuccess("The client sent max_age=0 and prompt=none as expected");
			return env;
		} else {
			throw error("Invalid parameters. This test requires max_age=0 and prompt=none parameters",
						args("max_age", maxAge, "prompt", prompt));
		}

	}

}
