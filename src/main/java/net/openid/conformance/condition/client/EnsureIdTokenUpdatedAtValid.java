package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIdTokenUpdatedAtValid extends AbstractUpdatedAtValid {
	public static final String location = "id_token";

	@Override
	@PreEnvironment(required = location )
	public Environment evaluate(Environment env) {
		return validateUpdatedAt(env, location);
	}

}
