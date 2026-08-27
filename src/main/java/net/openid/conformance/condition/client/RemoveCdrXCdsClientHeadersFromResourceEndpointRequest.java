package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveCdrXCdsClientHeadersFromResourceEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		headers.remove("x-cds-client-headers");

		logSuccess("Removed x-cds-client-headers from resource endpoint request headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}
