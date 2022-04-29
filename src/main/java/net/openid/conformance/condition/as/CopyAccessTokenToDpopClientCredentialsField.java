package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyAccessTokenToDpopClientCredentialsField extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_access_token")
	@PostEnvironment(required = "dpop_client_credentials_access_token")
	public Environment evaluate(Environment env) {
		env.putObject("dpop_client_credentials_access_token", env.getObject("dpop_access_token"));

		return env;
	}

}
