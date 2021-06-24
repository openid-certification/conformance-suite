package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToDeleteConsent extends AbstractCondition {

	 @Override
	 public Environment evaluate(Environment env) {
	 	env.putString("http_method", "DELETE");
	 	env.putBoolean("expect_response_body", false);
		return env;
	 }

 }
