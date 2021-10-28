package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class StoreScope extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String endpoint = env.getString("client", "scope");
		env.putString("starting_scope", endpoint);
		logSuccess("Successfully set the scope to default " + endpoint);
		return env;
	}
}
