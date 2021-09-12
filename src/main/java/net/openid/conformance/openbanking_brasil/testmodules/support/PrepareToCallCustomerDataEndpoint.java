package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToCallCustomerDataEndpoint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String url = env.getString("config", "resource.customerUrl");
		String protectedUrl = String.format("%s/%s", url, "personal/financial-relations");
		env.putString("protected_resource_url", protectedUrl);
		return env;
	}

}
