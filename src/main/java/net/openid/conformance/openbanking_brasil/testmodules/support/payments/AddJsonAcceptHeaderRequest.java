package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddJsonAcceptHeaderRequest extends AbstractCondition {
	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("resource_endpoint_request_headers", headers);
		}

		headers.addProperty("accept", "application/json");

		logSuccess("Added application/json accept field to resource endpoint request headers",
			args("resource_endpoint_request_headers", headers));

		return env;
	}
}
