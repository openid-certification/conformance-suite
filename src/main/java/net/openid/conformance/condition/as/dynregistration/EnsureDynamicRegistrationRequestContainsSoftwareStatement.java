package net.openid.conformance.condition.as.dynregistration;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureDynamicRegistrationRequestContainsSoftwareStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		String softwareStatement = env.getString("dynamic_registration_request", "software_statement");
		if (Strings.isNullOrEmpty(softwareStatement)) {
			throw error("The registration request does not contain a software_statement",
				args("registration_request", env.getObject("dynamic_registration_request")));
		}
		logSuccess("The registration request contains a software_statement");
		return env;
	}
}
