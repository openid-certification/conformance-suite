package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureOpenIDInScopeRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("openid")) {
			logSuccess("Found 'openid' scope in request", args("expected", "openid", "actual", scopes));
			return env;
		} else {
			throw error("Coudln't find 'openid' scope in request", args("expected", "openid", "actual", scopes));
		}
	}

}
