package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

public class EnsureDataArrayIsEmpty extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {

		JsonObject body = JsonParser.parseString(env.getString("resource_endpoint_response_full", "body"))
			.getAsJsonObject();
		JsonArray dataArray = body.getAsJsonArray("data");

		if(!dataArray.isEmpty()) {
			throw error("Data array should be empty, but it was not.", args("data", dataArray));
		}

		logSuccess("Data array is empty.");
		return env;
	}
}
