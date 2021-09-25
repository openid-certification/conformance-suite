package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ClearRequestObjectFromEnvironment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.removeNativeValue("resource_request_entity");
		logSuccess("Cleared out request entity for future calls");
		return env;
	}

}
