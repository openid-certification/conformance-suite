package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureScopeContainsAtLeastOneOfProfileEmailPhoneAddress extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("profile") || scopes.contains("email") || scopes.contains("phone") || scopes.contains("address")) {
			logSuccess("Found at least one of profile, email, phone and address " +
				"scopes in request", args("actual", scopes));
			return env;
		} else {
			throw error("Could not find at least one of profile, email, phone and address scope in request", args("actual", scopes));
		}
	}
}
