package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectJWTResponse extends AbstractCondition {

	@Override
	@PostEnvironment(strings = {"expect_jwt"})
	public Environment evaluate(Environment env) {
		env.putString("expect_jwt", "true");
		log("Expecting jwt from endpoint response");
		return env;
	}

}
