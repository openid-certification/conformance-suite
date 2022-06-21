package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.util.Map;

public class ExtractResourceIdOfResourcesWithSpecifiedTypeAndStatus extends AbstractCondition {
	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	@PreEnvironment(strings = {"resource_type", "resource_status", "resource_endpoint_response"})
	@PostEnvironment(required = "extracted_resource_id")
	public Environment evaluate(Environment env) {
		String resource = env.getString("resource_endpoint_response");
		JsonObject body = GSON.fromJson(resource, JsonObject.class);
		JsonArray data = body.getAsJsonArray("data");
		String requiredType = env.getString("resource_type");
		String requiredStatus = env.getString("resource_status");
		JsonArray extractedIds = new JsonArray();
		for (JsonElement jsonElement : data) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			String resourceType = OIDFJSON.getString(jsonObject.get("type"));
			String resourceStatus = OIDFJSON.getString(jsonObject.get("status"));
			if (resourceType.equals(requiredType) && resourceStatus.equals(requiredStatus)) {
				if (jsonObject.has("resourceId")) {
					String resourceId = OIDFJSON.getString(jsonObject.get("resourceId"));
					extractedIds.add(resourceId);
				} else {
					throw error("Extracted resource does not have resourceId", Map.of("Resource Body", jsonObject));
				}

			}
		}


		JsonObject object = new JsonObject();
		object.add("extractedResourceIds", extractedIds);
		env.putObject("extracted_resource_id", object);
		logSuccess("Extracted resourceId's of corresponding API", Map.of("Resource type", requiredType, "Resource Status", requiredStatus, "Extracted resourceId", extractedIds));

		return env;
	}
}
