package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

public class SetProtectedResourceUrlToSelfEndpoint extends AbstractCondition {

	private static Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String entityString = env.getString("resource_endpoint_response");
		JsonObject body = GSON.fromJson(entityString, JsonObject.class);

		JsonObject links = body.getAsJsonObject("links");

		env.putString("protected_resource_url", OIDFJSON.getString(links.get("self")));

		log("Saving old environment values");
		return env;
	}
}

