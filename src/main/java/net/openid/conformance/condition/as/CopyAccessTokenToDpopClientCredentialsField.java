package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyAccessTokenToDpopClientCredentialsField extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "access_token")
	@PostEnvironment(strings = "dpop_client_credentials_access_token")
	public Environment evaluate(Environment env) {
		env.putString("dpop_client_credentials_access_token", env.getString("access_token"));

		return env;
	}

}
