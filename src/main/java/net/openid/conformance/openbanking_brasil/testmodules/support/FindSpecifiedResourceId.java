package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class FindSpecifiedResourceId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "extracted_resource_id", strings = "resource_id")
	public Environment evaluate(Environment env) {
		JsonArray resourceIds = env.getElementFromObject("extracted_resource_id", "extractedResourceIds").getAsJsonArray();
		String requiredResourceId = env.getString("resource_id");
		env.putBoolean("resource_found", false);
		resourceIds.forEach(el -> {
			String id = OIDFJSON.getString(el);
			if (id.equals(requiredResourceId)) {
				env.putBoolean("resource_found", true);
				logSuccess("Specified ID was found", Map.of("ID", requiredResourceId));
			}
		});
		return env;
	}
}
