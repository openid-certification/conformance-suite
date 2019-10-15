package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;

public class SetPermissiveAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		if (requestHeaders == null) {
			requestHeaders = new JsonObject();
			env.putObject("resource_endpoint_request_headers", requestHeaders);
		}

		requestHeaders.addProperty(HttpHeaders.ACCEPT, "application/json, application/*+json, */*");

		logSuccess("Set Accept header", args("Accept", requestHeaders.get("Accept")));

		return env;
	}

}
