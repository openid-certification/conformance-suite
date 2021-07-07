package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class RememberOriginalScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client", strings = "original_scopes")
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		String scope = OIDFJSON.getString(client.get("scope"));
		if (Strings.isNullOrEmpty(scope)) {
			throw error("scope missing/empty in client object");
		}
		env.putString("original_scopes", scope);
		return env;
	}

}
