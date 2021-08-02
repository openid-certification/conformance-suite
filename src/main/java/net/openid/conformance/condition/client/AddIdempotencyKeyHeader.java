package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIdempotencyKeyHeader extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "idempotency_key")
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("resource_endpoint_request_headers", headers);
		}

		String idempotencyKey = env.getString("idempotency_key");
		headers.addProperty("x-idempotency-key", idempotencyKey);

		logSuccess("Added x-idempotency-key to resource endpoint request headers",
			args("resource_endpoint_request_headers", headers));

		return env;
	}

}
