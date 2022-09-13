package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Optional;

public class ExtractToggleableScope extends AbstractCondition {

	@PreEnvironment(required = "config")
	@Override
	public Environment evaluate(Environment env) {
		JsonObject client = (JsonObject) env.getElementFromObject("config", "client");
		String scope = OIDFJSON.getString(Optional.ofNullable(client.get("toggleable_scope"))
			.orElseThrow(() -> error("Could not find toggleable_scope in the client config", args("client", client))));
		if(Strings.isNullOrEmpty(scope)){
			throw error("toggleable_scope has to be specified in the client config", args("client", client));
		}
		scope = "openid " + scope;
		client.addProperty("scope", scope);
		logSuccess("Added client scope", args("scope", scope));
		return env;
	}
}
