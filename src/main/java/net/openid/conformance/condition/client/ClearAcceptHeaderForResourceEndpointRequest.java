package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

public class ClearAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		if (requestHeaders != null) {
			requestHeaders.remove(HttpHeaders.ACCEPT);
		}

		logSuccess("Clearing custom Accept header");

		return env;
	}

}
