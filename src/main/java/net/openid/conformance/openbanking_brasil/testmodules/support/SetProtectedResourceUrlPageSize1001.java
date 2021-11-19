package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetProtectedResourceUrlPageSize1001 extends AbstractCondition {

	@Override
	
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceUrl = env.getString("protected_resource_url");
		env.putString("protected_resource_url", resourceUrl.concat("?page-size=1001"));

		return env;
	}
}

