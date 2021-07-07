package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ResetScopesToConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "original_scopes", required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		String scope = env.getString("original_scopes");
		client.addProperty("scope", scope);

		logSuccess("Scopes reset", args("scope", scope, "client", client));

		return env;
	}
}
