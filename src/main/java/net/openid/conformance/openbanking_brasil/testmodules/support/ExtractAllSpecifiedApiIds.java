package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonUtils;

import java.util.Map;

public class ExtractAllSpecifiedApiIds extends AbstractCondition {
	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	@PreEnvironment(strings = "apiIdName")
	@PostEnvironment(required = "extracted_api_ids")
	public Environment evaluate(Environment env) {
		JsonArray extractedApiIds = new JsonArray();
		String entityString = env.getString("resource_endpoint_response");
		String specifiedApiIdName = env.getString("apiIdName");
		JsonObject accountList = GSON.fromJson(entityString, JsonObject.class);
		JsonArray data = accountList.getAsJsonArray("data");
		for (JsonElement jsonElement : data) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			extractedApiIds.add(jsonObject.get(specifiedApiIdName));
		}
		if(!extractedApiIds.isEmpty()){
			JsonObject object = new JsonObject();
			object.add("extractedApiIds", extractedApiIds);
			env.putObject("extracted_api_ids", object);
			logSuccess("Extracted all API IDs", Map.of("Extracted ID's", extractedApiIds));
		}else {
			throw error("No API IDs were extracted", Map.of("data", data));
		}

		return env;
	}
}
