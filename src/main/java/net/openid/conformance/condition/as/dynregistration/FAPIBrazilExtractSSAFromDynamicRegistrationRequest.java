package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilExtractSSAFromDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	@PostEnvironment(strings = { "software_statement_assertion"})
	public Environment evaluate(Environment env) {

		String ssa = env.getString("dynamic_registration_request", "software_statement");
		env.putString("software_statement_assertion", ssa);
		logSuccess("Extracted software statement assertion from dynamic client registration request",
			args("software_statement", ssa));
		return env;
	}
}
