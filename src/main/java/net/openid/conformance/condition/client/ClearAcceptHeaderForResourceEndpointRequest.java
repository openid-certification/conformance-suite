package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class ClearAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		requestHeaders.remove(HttpHeaders.ACCEPT);

		logSuccess("Cleared custom Accept header");

		return env;
	}

}
