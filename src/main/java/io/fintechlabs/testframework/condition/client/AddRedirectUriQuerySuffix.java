package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddRedirectUriQuerySuffix extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client", strings = "client_id")
	public Environment evaluate(Environment env) {

		env.putString("redirect_uri_suffix", "?dummy1=lorem&dummy2=ipsum");

		return env;
	}

}
