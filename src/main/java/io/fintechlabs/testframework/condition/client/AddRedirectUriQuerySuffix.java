package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddRedirectUriQuerySuffix extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String redirectUriSuffix = "?dummy1=lorem&dummy2=ipsum";

		env.putString("redirect_uri_suffix", redirectUriSuffix);

		logSuccess("Created redirect URI query suffix", args("redirect_uri_suffix", redirectUriSuffix));

		return env;
	}

}
