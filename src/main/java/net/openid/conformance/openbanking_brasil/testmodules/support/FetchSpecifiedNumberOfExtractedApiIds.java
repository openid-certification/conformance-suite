package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class FetchSpecifiedNumberOfExtractedApiIds extends AbstractCondition {
	@Override
	@PreEnvironment(required = "extracted_api_ids")
	@PostEnvironment(required = "fetched_api_ids")
	public Environment evaluate(Environment env) {
		Integer numberOfIdsToFetch = env.getInteger("number_of_ids_to_fetch");

		if (numberOfIdsToFetch != null) {
			JsonArray extractedApiIds = env.getObject("extracted_api_ids").getAsJsonArray("extractedApiIds");
			if (extractedApiIds.size() >= numberOfIdsToFetch) {
				JsonArray fetchedApiIds = new JsonArray();
				for (int i = 0; i < numberOfIdsToFetch; i++) {
					fetchedApiIds.add(extractedApiIds.get(i));
				}
				JsonObject jsonObject = new JsonObject();
				jsonObject.add("fetchedApiIds", fetchedApiIds);
				env.putObject("fetched_api_ids", jsonObject);
				logSuccess("Fetched specified number of API IDs",
					Map.of("Required number", numberOfIdsToFetch, "Fetched IDs", fetchedApiIds));
			} else {
				throw error("The number of extracted API IDs is less than required",
					Map.of("Required", numberOfIdsToFetch, "Actual", extractedApiIds.size()));
			}
		}else {
			throw error("Could not find number of ids to fetch in the environment. This is a bug");
		}

		return env;
	}
}
