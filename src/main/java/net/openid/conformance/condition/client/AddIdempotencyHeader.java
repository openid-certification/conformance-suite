package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIdempotencyHeader extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "idempotency_key")
	@PostEnvironment(required = "token_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("token_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("token_endpoint_request_headers", headers);
		}

		String idempotencyKey = env.getString("idempotency_key");
		headers.addProperty("x-idempotency-key", idempotencyKey);

		logSuccess("Added x-idempotency-key to token endpoint request headers",
			args("token_endpoint_request_headers", headers));

		return env;
	}

}
