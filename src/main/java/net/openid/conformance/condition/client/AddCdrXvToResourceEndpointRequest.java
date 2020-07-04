package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCdrXvToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "config", "resource_endpoint_request_headers" } )
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		String version = env.getString("config", "resource.cdrVersion");
		if (Strings.isNullOrEmpty(version)) {
			throw error("cdrVersion empty/missing from resource section of configuration");
		}
		headers.addProperty("x-v", version);

		log("Added x-v to resource_endpoint_request_headers");
		return env;
	}

}
