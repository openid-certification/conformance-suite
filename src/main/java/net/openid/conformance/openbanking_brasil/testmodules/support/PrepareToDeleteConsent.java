package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareToDeleteConsent extends AbstractCondition {

	 @Override
	 @PostEnvironment(strings = "http_method")
	 public Environment evaluate(Environment env) {
	 	env.putString("http_method", "DELETE");
	 	env.putBoolean("expect_response_body", false);
	 	log("Call to consents API will be an HTTP DELETE. We do not expect a response body");
		return env;
	 }

 }
