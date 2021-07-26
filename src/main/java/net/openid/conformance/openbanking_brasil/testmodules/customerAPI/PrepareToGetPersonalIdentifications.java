package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToGetPersonalIdentifications extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String url = env.getString("config", "resource.resourceUrl");
		String protectedUrl = String.format("%s/%s", url, "personal/identifications");
		env.putString("protected_resource_url", protectedUrl);
		return env;
	}

}
