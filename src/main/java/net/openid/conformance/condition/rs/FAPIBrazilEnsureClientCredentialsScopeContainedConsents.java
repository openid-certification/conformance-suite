package net.openid.conformance.condition.rs;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPIBrazilEnsureClientCredentialsScopeContainedConsents extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"scope_in_client_credentials_grant"})
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope_in_client_credentials_grant");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains("consents")) {
			logSuccess("The token request which was used to obtain the access token contained 'consents' scope", args( "actual", scopes));
			return env;
		} else {
			throw error("The token request which was used to obtain the access token did not contain 'consents' scope." +
				" This endpoint requires consents scope", args("expected", "payments", "actual", scopes));
		}
	}

}
