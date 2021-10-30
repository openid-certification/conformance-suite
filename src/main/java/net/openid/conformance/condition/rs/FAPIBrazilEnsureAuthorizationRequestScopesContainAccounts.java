package net.openid.conformance.condition.rs;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"scope"})
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("accounts")) {
			logSuccess("'accounts' was included in authorization request scopes",
				args("expected", "accounts", "actual", scope));
			return env;
		} else {
			throw error("'accounts'  was not included included in authorization request scopes",
				args("expected", "accounts", "actual", scope));
		}
	}

}
