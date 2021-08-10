package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RequireResponseBody extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.putBoolean("expect_response_body", true);
		log("Requiring a response body");
		return env;
	}
}
