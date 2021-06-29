package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddScopesForRegistrationApi extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject clientConfig = env.getObject("client");

		String scope = "openid customers";

		clientConfig.addProperty("scope", scope);
		return env;
	}
}
