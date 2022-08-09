package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

abstract public class AbstractSaveResourceData extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	@PostEnvironment(required = "resource_data")
	public Environment evaluate(Environment env) {
		JsonObject body = JsonParser.parseString(env.getString("resource_endpoint_response_full", "body"))
			.getAsJsonObject();
		JsonArray dataArray = body.getAsJsonArray("data");

		JsonObject resourceData = searchResource(dataArray);
		env.putObject("resource_data", resourceData);

		logSuccess("Resource saved successfully.", resourceData);

		return env;
	}

	abstract protected JsonObject searchResource(JsonArray data);
}
