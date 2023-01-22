package net.openid.conformance.condition.rs;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPIBrazilEnsureAuthorizationRequestScopesContainResources extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"scope"})
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("resources")) {
			logSuccess("'resources' was included in authorization request scopes",
				args("expected", "resources", "actual", scope));
			return env;
		} else {
			throw error("'resources'  was not included included in authorization request scopes",
				args("expected", "resources", "actual", scope));
		}
	}

}
