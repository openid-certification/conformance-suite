package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class CompareResourceIdWithAPIResourceId extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"extracted_api_ids", "extracted_resource_id"})
	public Environment evaluate(Environment env) {
		JsonArray extractedApiIds = env.getElementFromObject("extracted_api_ids", "extractedApiIds").getAsJsonArray();
		JsonArray extractedResourcesIds = env.getElementFromObject("extracted_resource_id", "extractedResourceIds").getAsJsonArray();

		if (extractedApiIds.size() == extractedResourcesIds.size()) {
			logSuccess("Sizes are equal", Map.of("ApiIdsSize", extractedApiIds.size(), "ResourceIdsSize", extractedResourcesIds.size()));
			for (JsonElement extractedResourcesId : extractedResourcesIds) {
				if (!extractedApiIds.contains(extractedResourcesId)) {
					throw error("API resources do not have a resource fetched from the resource endpoint response",
						Map.of("Missing ID", extractedResourcesId,
							"extractedApiIds", extractedApiIds,
							"extractedResourceIds", extractedResourcesIds)
					);
				}
			}

			for (JsonElement extractedApiId : extractedApiIds) {
				if (!extractedResourcesIds.contains(extractedApiId)) {
					throw error("API resources do not have a resource fetched from the resource endpoint response",
						Map.of("Missing ID", extractedApiId,
							"extractedApiIds", extractedApiIds,
							"extractedResourceIds", extractedResourcesIds)
					);
				}
			}
		} else {
			throw error("Sizes of two resource lists are not equal", Map.of(
				"ApiIdsSize", extractedApiIds.size(),
				"ResourceIdsSize", extractedResourcesIds.size(),
				"extractedApiIds", extractedApiIds,
				"extractedResourceIds", extractedResourcesIds)
			);
		}

		logSuccess("resourceId values are identical");
		return env;
	}
}
