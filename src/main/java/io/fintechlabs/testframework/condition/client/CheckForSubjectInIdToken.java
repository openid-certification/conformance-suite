package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForSubjectInIdToken extends AbstractCondition {

	public Environment evaluate(Environment env) {

		String sub = env.getString("id_token", "claims.sub");

		if (!Strings.isNullOrEmpty(sub)) {
			logSuccess("Found 'sub' in id_token", args("sub", sub));
			return env;
		} else {
			throw error("id_token does not contain 'sub'");
		}
	}

}
