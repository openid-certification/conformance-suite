package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToCallCustomerDataEndpoint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String url = env.getString("config", "resource.customerUrl");
		if (Strings.isNullOrEmpty(url)) {
			throw error("Path resource.customerUrl not available in the config");
		}
		env.putString("protected_resource_url", url);
		return env;
	}

}
