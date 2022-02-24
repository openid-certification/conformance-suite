package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class SetApplicationJwtContentTypeHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		requestHeaders.addProperty(HttpHeaders.CONTENT_TYPE, DATAUTILS_MEDIATYPE_APPLICATION_JWT.toString());

		logSuccess("Set Content-Type header", args("content_type", requestHeaders.get(HttpHeaders.CONTENT_TYPE)));

		return env;
	}

}
