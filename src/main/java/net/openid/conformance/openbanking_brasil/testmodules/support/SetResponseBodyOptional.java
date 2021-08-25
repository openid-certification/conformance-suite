package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetResponseBodyOptional extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.putBoolean("expect_response_body", true);
		env.putBoolean("optional_response_body", true);
		log("Making response body optional");
		return env;
	}
}
