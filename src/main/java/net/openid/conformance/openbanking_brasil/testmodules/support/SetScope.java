package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetScope extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String endpoint = env.getString("starting_scope");
		env.getObject("client").addProperty("scope", endpoint);
		logSuccess("Successfully set the scope to default " + endpoint);
		return env;
	}
}
