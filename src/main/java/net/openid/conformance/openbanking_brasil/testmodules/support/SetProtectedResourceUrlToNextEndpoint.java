package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

public class SetProtectedResourceUrlToNextEndpoint extends AbstractCondition {

	private static Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String entityString;
		JsonObject body;
		try {
			entityString = env.getString("resource_endpoint_response");
			body = GSON.fromJson(entityString, JsonObject.class);
		} catch (JsonSyntaxException e) {
			body = env.getObject("resource_endpoint_response");
		}

		JsonObject links = body.getAsJsonObject("links");

		log("Ensure that there is a link to next.");
		if (!JsonHelper.ifExists(body, "$.links.next")) {
			log("'Next' link not found in the response.");
			throw error("'Next' link not found in the response.");
		}
		
		env.putString("protected_resource_url", OIDFJSON.getString(links.get("next")));

		return env;
	}
}

