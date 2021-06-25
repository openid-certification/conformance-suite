package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareToPostConsentRequest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "http_method")
	public Environment evaluate(Environment env) {
		env.putString("http_method", "POST");
		logSuccess("Call to consent API will be an HTTP POST");
		return env;
	}
}
