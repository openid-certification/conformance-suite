package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CopyAccessTokenToClientCredentialsField extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "access_token")
	@PostEnvironment(strings = "client_credentials_access_token")
	public Environment evaluate(Environment env) {
		env.putString("client_credentials_access_token", env.getString("access_token"));

		return env;
	}

}
