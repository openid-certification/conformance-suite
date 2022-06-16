package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class EnsureSpecifiedIdIsPresent extends AbstractCondition {
	@Override
	@PreEnvironment(required = "extracted_api_ids", strings = "resource_id")
	public Environment evaluate(Environment env) {

		JsonArray extractedApiIds = env.getElementFromObject("extracted_api_ids", "extractedApiIds").getAsJsonArray();
		String requiredId = env.getString("resource_id");
		boolean isIdFound = false;
		for (JsonElement el : extractedApiIds) {
			String extractedId = OIDFJSON.getString(el);
			if (extractedId.equals(requiredId)) {
				isIdFound = true;
			}
		}
		if (isIdFound) {
			logSuccess("Required ID is present", Map.of("Required ID", requiredId, "Extracted IDs", extractedApiIds));
		}else {
			throw error("Required ID is not present", Map.of("Required ID", requiredId, "Extracted IDs", extractedApiIds));
		}
		return env;
	}
}
