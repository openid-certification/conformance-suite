package net.openid.conformance.openbanking_brasil.testmodules.support.http;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class EnsureHttpStatusCodeIsAllowed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"allowedHttpStatuses", "resource_endpoint_response_full"})
	public Environment evaluate(Environment env) {
		JsonObject allowedStatuses = env.getObject("allowedHttpStatuses");
		JsonObject endpointResponse = env.getObject("resource_endpoint_response_full");
		List<Integer> statuses = OIDFJSON.getIntArray(allowedStatuses.get("statuses"));
		int actualStatus = OIDFJSON.getInt(endpointResponse.get("status"));
		if(statuses.contains(actualStatus)) {
			logSuccess("Response status was in the allowed range", Map.of("allowed", statuses, "actual", actualStatus));
		} else {
			throw error("Response status was not in the allowed range", Map.of("allowed", statuses, "actual", actualStatus));
		}
		return env;
	}
}
