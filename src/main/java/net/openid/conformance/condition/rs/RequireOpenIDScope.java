package net.openid.conformance.condition.rs;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

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
