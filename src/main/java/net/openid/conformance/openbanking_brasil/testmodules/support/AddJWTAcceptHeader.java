package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddJWTAcceptHeader extends AbstractCondition {

	@Override
	@PostEnvironment(strings = {"accept_type"})
	public Environment evaluate(Environment env) {
		env.putString("accept_type", "application/jwt");
		log("Adding jwt to accept header");
		return env;
	}

}
