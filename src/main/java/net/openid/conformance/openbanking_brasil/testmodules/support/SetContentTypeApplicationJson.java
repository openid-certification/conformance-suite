package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;

public class SetContentTypeApplicationJson extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		headers.addProperty("content-type", MediaType.APPLICATION_JSON_VALUE);

		log("Call to consents API will be an HTTP DELETE. We do not expect a response body");
		return env;
	}

}
