package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class ExtractFirstResourceIdToSpecifiedEnvironmentKey extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "environment_key", required = "extracted_resource_id")
	public Environment evaluate(Environment env) {
		JsonArray resourceIds = env.getElementFromObject("extracted_resource_id", "extractedResourceIds").getAsJsonArray();
		if (!resourceIds.isEmpty()) {
			String resourceId = OIDFJSON.getString(resourceIds.get(0));
			String environmentKey = env.getString("environment_key");
			env.putString(environmentKey, resourceId);
			logSuccess("Resource id was extracted", Map.of("Resource id", resourceId, "Environment Key", environmentKey));
		}else {
			throw error("Resource IDs Array is empty", Map.of("Array", resourceIds));
		}
		return env;
	}
}
