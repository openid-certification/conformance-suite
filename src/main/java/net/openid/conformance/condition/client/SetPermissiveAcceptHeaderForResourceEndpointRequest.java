package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class SetPermissiveAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	@PreEnvironment(required = "resource_endpoint_request_headers")
	@Override
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		requestHeaders.addProperty(HttpHeaders.ACCEPT, "application/json, application/*+json, */*");

		logSuccess("Set Accept header", args("Accept", requestHeaders.get("Accept")));

		return env;
	}

}
