package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToGetBusinessFinancialRelations extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String url = env.getString("config", "resource.resourceUrl");
		url = String.format("%s/%s", url, "business/financial-relations");
		env.putString("protected_resource_url", url);
		return env;
	}

}
