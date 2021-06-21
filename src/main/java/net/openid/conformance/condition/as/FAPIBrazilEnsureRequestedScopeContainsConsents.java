package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPIBrazilEnsureRequestedScopeContainsConsents extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_request"})
	public Environment evaluate(Environment env) {

		String scope = env.getString("token_endpoint_request", "body_form_params.scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("consents")) {
			logSuccess("Found 'consents' scope in request", args("expected", "consents", "actual", scopes));
			return env;
		} else {
			throw error("Couldn't find 'consents' scope in request", args("expected", "consents", "actual", scopes));
		}
	}

}
