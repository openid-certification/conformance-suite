package io.fintechlabs.testframework.condition.rs;

import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class RequireOpenIDScope extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {
		String scope = env.getString("scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope));

		if (!scopes.contains("openid")) {
			throw error("Couldn't find openid scope", args("scopes", scopes));
		} else {
			logSuccess("Found openid scope in scopes list", args("scopes", scopes));
			return env;
		}
	}

}
