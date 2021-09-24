package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ClearRequestObjectFromEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("resource_request_entity");
		logSuccess("Cleared out request entity for future calls");
		return env;
	}

}
