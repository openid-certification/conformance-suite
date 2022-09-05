package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureUserInfoUpdatedAtValid extends AbstractUpdatedAtValid {
	public static final String location = "userinfo";

	@Override
	@PreEnvironment(required = location )
	public Environment evaluate(Environment env) {
		return validateUpdatedAt(env, location);
	}

}
