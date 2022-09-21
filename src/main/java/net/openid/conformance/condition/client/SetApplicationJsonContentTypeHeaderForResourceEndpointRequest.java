package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class SetApplicationJsonContentTypeHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		requestHeaders.addProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		logSuccess("Set Content-Type header", args("content_type", requestHeaders.get(HttpHeaders.CONTENT_TYPE)));

		return env;
	}

}
