package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_request"})
	@PostEnvironment(strings = {"scope_in_client_credentials_grant"})
	public Environment evaluate(Environment env) {

		String scope = env.getString("token_endpoint_request", "body_form_params.scope");

		if (scope == null) {
			throw error("Scope parameter not found in request", args("expected one of", "payments, consents", "actual", scope));
		}

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("payments")) {
			logSuccess("Found 'payments' scope in request", args("expected", "payments", "actual", scopes));
			env.putString("scope_in_client_credentials_grant", scope);
			return env;
		} else if (scopes.contains("consents")) {
			logSuccess("Found 'consents' scope in request", args("expected", "consents", "actual", scopes));
			env.putString("scope_in_client_credentials_grant", scope);
			return env;
		} else {
			throw error("Couldn't find either 'consents' or 'payments' scope in request", args("expected", "consents", "actual", scopes));
		}
	}

}
