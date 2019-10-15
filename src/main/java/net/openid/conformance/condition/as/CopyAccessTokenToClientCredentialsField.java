package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyAccessTokenToClientCredentialsField extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "access_token")
	@PostEnvironment(strings = "client_credentials_access_token")
	public Environment evaluate(Environment env) {
		env.putString("client_credentials_access_token", env.getString("access_token"));

		return env;
	}

}
